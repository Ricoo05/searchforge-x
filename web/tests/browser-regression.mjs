/** Run through Codex's documented CUA browser API against the isolated 5274 preview. */
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';

export async function runBrowserRegression(tab, browser, root) {
  assert.match(await tab.url(), /^http:\/\/127\.0\.0\.1:5274\//, 'Writes must stay in the isolated test preview');
  const startedAt=new Date().toISOString();
  const evidence=path.join(root,'evidence','ui-after');
  const baseline=JSON.parse(await fs.readFile(path.join(evidence,'browser-reference.json'),'utf8'));
  const checks=[];
  const pass=(name,details={})=>checks.push({name,passed:true,...details});
  const snapshot=()=>tab.getAXState({emit:false});
  const p=tab.playwright;
  const search=async(query,enter=false)=>{
    await p.getByLabel('Search query',{exact:true}).fill(query);
    if(enter)await p.getByLabel('Search query',{exact:true}).press('Enter');
    else await p.getByRole('button',{name:'Run search',exact:true}).click();
    await p.locator('.results-toolbar').waitFor({state:'visible'});
    await snapshot();
  };
  const resultRows=()=>p.locator('.result-card').evaluateAll(rows=>rows.map(row=>({
    metadata:row.querySelector('.result-meta')?.textContent,
    title:row.querySelector('h3')?.textContent,text:row.querySelector('.result-body p')?.textContent,
  })));
  const checkRows=async(expected)=>{
    const actual=await resultRows();assert.equal(actual.length,expected.length);
    actual.forEach((row,i)=>{
      assert.equal(row.title,expected[i].document.metadata.title);
      assert.equal(row.text,expected[i].document.text);
      assert.match(row.metadata,new RegExp(`DOC ${expected[i].id}SHARD ${expected[i].id%3}BM25 ${expected[i].score.toFixed(4).replace('.','\\.')}$`));
    });
  };
  await search('parityfixture20261102',true);
  await checkRows(baseline.fixtureResult.hits.slice(0,10));
  assert.equal(await p.getByRole('button',{name:'← Previous',exact:true}).isEnabled(),false);
  pass('Enter submission, first page exact IDs/order/scores/titles/literal HTML/Unicode');
  await p.getByRole('button',{name:'Next →',exact:true}).click();
  await p.getByText('PAGE 2',{exact:true}).waitFor({state:'visible'});await snapshot();
  await checkRows(baseline.fixtureResult.hits.slice(10,20));pass('Second page exact ranked rows');
  await p.getByRole('button',{name:'Next →',exact:true}).click();
  await p.getByText('PAGE 3',{exact:true}).waitFor({state:'visible'});await snapshot();
  await checkRows(baseline.fixtureResult.hits.slice(20,25));
  assert.equal(await p.getByRole('button',{name:'Next →',exact:true}).isEnabled(),false);
  pass('Last page five rows, long title, next disabled');
  await p.getByRole('button',{name:'← Previous',exact:true}).click();
  await p.getByText('PAGE 2',{exact:true}).waitFor({state:'visible'});await snapshot();
  await checkRows(baseline.fixtureResult.hits.slice(10,20));pass('Previous page preserves results');
  for(const query of ['"inverted index"','recovery','search AND ranking']) {
    await p.locator('.suggestions button').filter({hasText:query}).click();
    await p.locator('.results-toolbar strong').getByText(query,{exact:true}).waitFor({state:'visible'});
    await snapshot();assert.match(await p.locator('.result-status').innerText(),/COMPLETE/);pass('Example query '+query);
  }
  await search('zzzznomatchverifiedzzzz');
  assert.equal(await p.locator('.result-card').count(),0);
  assert.equal(await p.getByRole('heading',{name:'No matches this time.',exact:true}).isVisible(),true);
  pass('Zero results guidance');
  await fs.writeFile(path.join(evidence,'zero-results.jpg'),await tab.screenshot({fullPage:true}));
  await p.getByLabel('Search query',{exact:true}).fill('AND');
  await p.getByRole('button',{name:'Run search',exact:true}).click();
  await p.getByRole('alert').waitFor({state:'visible'});await snapshot();
  assert.match(await p.getByRole('alert').innerText(),/Request interrupted/);pass('Real parser error alert');
  await fs.writeFile(path.join(evidence,'invalid-query.jpg'),await tab.screenshot({fullPage:true}));
  await p.getByRole('button',{name:'Document studio',exact:true}).click();await snapshot();
  await p.getByLabel('Document ID',{exact:true}).fill('0');
  await p.getByLabel('Title',{exact:true}).fill('UI parity document');
  await p.getByLabel('Document text',{exact:true}).fill('uiparitywrite20261102 Literal <script>untrusted text</script> café 東京.');
  await p.getByRole('button',{name:'Index document',exact:true}).click();await snapshot();
  const validity=await p.getByLabel('Document ID',{exact:true}).evaluate(el=>({valid:el.validity.valid,underflow:el.validity.rangeUnderflow}));
  assert.equal(validity.valid,false);assert.equal(validity.underflow,true);pass('Native document ID validation');
  await p.getByLabel('Document ID',{exact:true}).fill('9100');
  assert.equal(await p.getByText('Routes to shard 1',{exact:true}).isVisible(),true);
  await p.getByRole('button',{name:'Index document',exact:true}).click();
  await p.getByText('INDEXING COMPLETE',{exact:true}).waitFor({state:'visible'});await snapshot();
  const proximity=await p.locator('.studio').evaluate(el=>({feedback:el.querySelector('.notice')?.getBoundingClientRect().top,
    action:el.querySelector('.studio-actions')?.getBoundingClientRect().bottom,sample:el.querySelector('.sample-strip')?.getBoundingClientRect().top}));
  assert(proximity.feedback>=proximity.action&&proximity.feedback<proximity.sample);
  pass('Real indexed document acknowledged beside action',proximity);
  await fs.writeFile(path.join(evidence,'index-success.jpg'),await tab.screenshot({fullPage:true}));
  await p.getByRole('button',{name:'Load sample corpus',exact:true}).click();
  await p.getByRole('button',{name:'Load sample corpus',exact:true}).waitFor({state:'visible'});await snapshot();
  await p.getByText('INDEXING COMPLETE',{exact:true}).waitFor({state:'visible'});pass('Original six-document sample load');
  await p.getByRole('button',{name:'Search explorer',exact:true}).click();await snapshot();
  await search('uiparitywrite20261102');
  assert.equal(await p.getByRole('heading',{name:'UI parity document',exact:true}).isVisible(),true);
  assert.match((await resultRows())[0].text,/<script>untrusted text<\/script> café 東京/);pass('Indexed text found and rendered as literal text');
  await p.locator('.scene-nodes button').nth(0).click();await snapshot();
  assert.equal(await p.getByRole('heading',{name:'Shard 00',exact:true}).isVisible(),true);
  await p.getByRole('button',{name:'Close shard inspection',exact:true}).click();await snapshot();
  assert.equal(await p.getByRole('heading',{name:'Cluster pulse',exact:true}).isVisible(),true);pass('3D keyboard alternative and inspector close');
  await p.getByRole('button',{name:'Pause 3D animation',exact:true}).click();await snapshot();
  await p.getByRole('button',{name:'Resume 3D animation',exact:true}).click();await snapshot();pass('Pause and resume controls');
  await p.getByRole('button',{name:'Refresh worker health',exact:true}).click();await snapshot();
  await p.getByText('3 of 3 workers responding',{exact:true}).waitFor({state:'visible'});pass('Live diagnostics refresh');
  await p.getByRole('button',{name:'Search explorer',exact:true}).press('/');await snapshot();
  assert.equal(await p.getByLabel('Search query',{exact:true}).evaluate(el=>el===document.activeElement),true);
  await p.getByLabel('Search query',{exact:true}).fill('slash');await p.getByLabel('Search query',{exact:true}).press('/');await snapshot();
  assert.equal(await p.getByLabel('Search query',{exact:true}).evaluate(el=>el.value),'slash/');pass('Slash shortcut respects text entry');
  const viewport=await browser.capabilities.get('viewport');
  const responsive=[];
  for(const width of [1440,1280,1024,768,390,320]) {
    await viewport.set({width,height:1000});
    await search('parityfixture20261102');
    for(const screen of ['search','index']) {
      if(screen==='index'){await p.getByRole('button',{name:'Document studio',exact:true}).click();await snapshot();}
      const geometry=await p.evaluate(()=>({width:innerWidth,scrollWidth:document.documentElement.scrollWidth,
        bodyWidth:document.body.scrollWidth,section:document.querySelector('.main-panel')?.getBoundingClientRect().width}));
      assert(geometry.scrollWidth<=geometry.width&&geometry.bodyWidth<=geometry.width,JSON.stringify(geometry));
      responsive.push({screen,...geometry});
      await fs.writeFile(path.join(evidence,`${screen}-${width}.jpg`),await tab.screenshot({fullPage:true}));
    }
    await p.getByRole('button',{name:'Search explorer',exact:true}).click();await snapshot();
  }
  pass('Twelve responsive screen checks without horizontal overflow',{responsive});
  await viewport.set({width:1440,height:1000});await snapshot();
  const logs=(await tab.dev.logs({levels:['info','error'],limit:300})).filter(item=>item.timestamp>=startedAt);
  const reports=logs.filter(x=>x.message.startsWith('SFX_AUDIT ')).map(x=>JSON.parse(x.message.slice(10)));
  const report={startedAt,testOrigin:'http://127.0.0.1:5274',backendOrigin:'http://127.0.0.1:8290',checks,axeReports:reports,
    consoleErrors:logs.filter(x=>x.level==='error'),note:'Local CUA checks; timeout, WebGL loss, reduced-motion OS setting and every ingestion outcome need separate verification.'};
  await fs.writeFile(path.join(evidence,'browser-regression.json'),JSON.stringify(report,null,2)+'\n');
  return report;
}
