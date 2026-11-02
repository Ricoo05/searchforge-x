"""Read-only parity check; fixtures may be written only to the isolated localhost:8290 copy."""
import argparse, hashlib, json, pathlib, urllib.request

def post(origin,path,body):
    request=urllib.request.Request(origin+path,data=json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(request,timeout=35) as response:return json.load(response)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=pathlib.Path,required=True);parser.add_argument('--seed-test-copy',action='store_true');args=parser.parse_args()
    original='http://127.0.0.1:8090';candidate='http://127.0.0.1:8290'
    queries=['search','"inverted index"','recovery','search AND ranking','zzzznomatchverifiedzzzz','"browser verification"']
    comparisons=[]
    for query in queries:
        request={'query':query,'offset':0,'limit':10,'timeoutMillis':5000}
        before=post(original,'/search',request);after=post(candidate,'/search',request)
        if before!=after:raise AssertionError('Copied-cluster result mismatch: '+query)
        comparisons.append({'query':query,'identical':True,'result':before})
    report={'readOnlyComparisons':comparisons,'productionDiagnostics':post(original,'/diagnostics',{}),'fixtureOrigin':candidate if args.seed_test_copy else None}
    if args.seed_test_copy:
        documents=[{'id':9000+i,'text':'parityfixture20261102 identical searchable fixture for ranking and pagination. <script>untrusted text</script> Unicode café 東京.',
                    'metadata':{'title':('Long document title with Unicode café and repeated explanatory context. '*10 if i==24 else f'Parity fixture {i:02d}')}} for i in range(25)]
        result=post(candidate,'/ingest',{'upserts':documents,'deletes':[]})
        if result['status']!='COMPLETE':raise AssertionError('Incomplete isolated fixture ingestion')
        report['fixtureResult']=post(candidate,'/search',{'query':'parityfixture20261102','offset':0,'limit':100,'timeoutMillis':5000})
    args.output.write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');print('software_parity_passed queries='+str(len(comparisons)))
