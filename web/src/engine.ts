import { useEffect, useRef, useState } from 'react';
export type Worker = { shard:number; status:string; documents?:number; terms?:number; tokens?:number; generation?:number };
export type Hit = {id:number;score:number;document:{text:string;metadata:Record<string,string>}};
export type SearchResult = {status:string;scoringScope:string;totalHits:number;hits:Hit[];missingShards:number[];generations:Record<string,number>};
export type IngestResult = {status:string;shards:{shard:number;status:string;generation?:number}[]};
type DocumentInput = {id:number;text:string;metadata:Record<string,string>};
export const samples: DocumentInput[] = [
  {id:1,metadata:{title:'How an inverted index works'},text:'An inverted index maps search terms to documents. Positional postings let the engine match an exact phrase.'},
  {id:2,metadata:{title:'Recovering after a process crash'},text:'Storage recovery validates checksums and committed generations. A process crash before publication leaves the previous generation available.'},
  {id:3,metadata:{title:'Ranking across three shards'},text:'Distributed search combines results from three workers. Global corpus statistics keep BM25 ranking comparable across shards.'},
  {id:4,metadata:{title:'Concurrent search and indexing'},text:'Immutable snapshots keep concurrent search consistent during indexing. Bounded worker pools provide backpressure under load.'},
  {id:5,metadata:{title:'When a worker is unavailable'},text:'A search request can produce partial results when a worker is unavailable. Partial results must be labeled and use available corpus statistics.'},
  {id:6,metadata:{title:'Compression with delta encoding'},text:'Delta encoding stores gaps between document IDs. Variable-length integers reduce the bytes needed for small gaps in posting lists.'},
];
async function post<T>(path:string, body:unknown, timeout=12000):Promise<T> {
  const response=await fetch(path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),signal:AbortSignal.timeout(timeout)});
  const value=await response.json();
  if(!response.ok) throw new Error(value.error??`Request failed (${response.status})`);
  return value as T;
}
export function useEngine() {
  const [workers,setWorkers]=useState<Worker[]>([]), [healthError,setHealthError]=useState('');
  const [checking,setChecking]=useState(true), [lastChecked,setLastChecked]=useState<Date|null>(null);
  const [query,setQuery]=useState(''), [executed,setExecuted]=useState('');
  const [result,setResult]=useState<SearchResult|null>(null), [busy,setBusy]=useState(false), [error,setError]=useState('');
  const [latency,setLatency]=useState<number|null>(null), [offset,setOffset]=useState(0);
  const [writing,setWriting]=useState(false), [writeResult,setWriteResult]=useState<IngestResult|null>(null), [writeError,setWriteError]=useState('');
  const [history,setHistory]=useState<{query:string;status:string;milliseconds:number}[]>([]);
  const refreshPending=useRef(false), queryPending=useRef(false), writePending=useRef(false);
  async function refresh() {
    if(refreshPending.current)return;refreshPending.current=true;
    try{setWorkers(await post<Worker[]>('/diagnostics',{}));setHealthError('');setLastChecked(new Date());}
    catch(e){setHealthError(e instanceof Error?e.message:String(e));setWorkers([]);}
    finally{setChecking(false);refreshPending.current=false;}
  }
  useEffect(()=>{void refresh();const timer=setInterval(()=>void refresh(),10000);return()=>clearInterval(timer);},[]);
  async function search(value:string,from=0) {
    if(!value.trim()||queryPending.current||writePending.current)return;
    queryPending.current=true;setBusy(true);setError('');setResult(null);setExecuted(value);setOffset(from);setLatency(null);
    const start=performance.now();let status='ERROR';
    try{const response=await post<SearchResult>('/search',{query:value,offset:from,limit:10,timeoutMillis:5000});setResult(response);status=response.status;}
    catch(e){setError(e instanceof Error?e.message:String(e));}
    finally{const milliseconds=performance.now()-start;setLatency(milliseconds);setHistory(previous=>[{query:value,status,milliseconds},...previous].slice(0,5));setBusy(false);queryPending.current=false;void refresh();}
  }
  async function ingest(upserts:DocumentInput[],deletes:number[]=[]) {
    if(writePending.current||queryPending.current)return;
    writePending.current=true;setWriting(true);setWriteError('');setWriteResult(null);setResult(null);
    try{setWriteResult(await post<IngestResult>('/ingest',{upserts,deletes},35000));}
    catch(e){setWriteError(`Write outcome uncertain. Check worker status before retrying. ${e instanceof Error?e.message:String(e)}`);}
    finally{setWriting(false);writePending.current=false;void refresh();}
  }
  return {workers,healthError,checking,lastChecked,refresh,query,setQuery,executed,result,busy,error,latency,offset,
    search,writing,writeResult,writeError,ingest,history,ready:workers.filter(worker=>worker.status==='ready')};
}
