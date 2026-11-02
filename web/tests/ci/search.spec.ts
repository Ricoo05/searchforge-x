import {test,expect} from '@playwright/test';

// Real backend, isolated CI Compose volumes. No intercepted/mocked responses.
test.beforeAll(async({request})=>{
  const response=await request.post('/ingest',{data:{upserts:Array.from({length:25},(_,i)=>({id:10000+i,
    text:'portfoliobrowserfixture literal <script>untrusted</script> café 東京.',metadata:{title:`Browser fixture ${i}`}})),deletes:[]}});
  expect(response.ok()).toBeTruthy();expect((await response.json()).status).toBe('COMPLETE');
});
test('ranked API rows, Enter, pagination and literal document text',async({page,request})=>{
  const reference=await request.post('/search',{data:{query:'portfoliobrowserfixture',offset:0,limit:10,timeoutMillis:5000}});
  const expected=await reference.json();await page.goto('/');await page.getByLabel('Search query',{exact:true}).fill('portfoliobrowserfixture');
  await page.getByLabel('Search query',{exact:true}).press('Enter');await expect(page.locator('.result-card')).toHaveCount(10);
  for(let i=0;i<10;i++){
    const row=page.locator('.result-card').nth(i),hit=expected.hits[i];
    await expect(row.locator('.result-meta')).toContainText(`DOC ${hit.id}`);
    await expect(row.locator('.score')).toHaveText(`BM25 ${hit.score.toFixed(4)}`);
    await expect(row.locator('h3')).toHaveText(hit.document.metadata.title);
    await expect(row.locator('.result-body p')).toHaveText(hit.document.text);
  }
  await expect(page.getByRole('button',{name:'← Previous',exact:true})).toBeDisabled();
  await page.getByRole('button',{name:'Next →',exact:true}).click();await expect(page.getByText('PAGE 2',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Next →',exact:true}).click();await expect(page.locator('.result-card')).toHaveCount(5);
  await expect(page.getByRole('button',{name:'Next →',exact:true})).toBeDisabled();
});
test('parser error and zero results',async({page})=>{
  await page.goto('/');await page.getByLabel('Search query',{exact:true}).fill('AND');await page.getByRole('button',{name:'Run search',exact:true}).click();
  await expect(page.getByRole('alert').first()).toContainText('Request interrupted');
  await page.getByLabel('Search query',{exact:true}).fill('absentportfoliotermzzz');await page.getByRole('button',{name:'Run search',exact:true}).click();
  await expect(page.getByRole('heading',{name:'No matches this time.',exact:true})).toBeVisible();
});
test('index real document and find it',async({page})=>{
  await page.goto('/');await page.getByRole('button',{name:'Document studio',exact:true}).click();
  await page.getByLabel('Document ID',{exact:true}).fill('9100');await page.getByLabel('Title',{exact:true}).fill('CI indexed document');
  await page.getByLabel('Document text',{exact:true}).fill('actualindexedbrowsertext');await page.getByRole('button',{name:'Index document',exact:true}).click();
  await expect(page.getByText('INDEXING COMPLETE',{exact:true})).toBeVisible();await page.getByRole('button',{name:'Search explorer',exact:true}).click();
  await page.getByLabel('Search query',{exact:true}).fill('actualindexedbrowsertext');await page.getByRole('button',{name:'Run search',exact:true}).click();
  await expect(page.getByRole('heading',{name:'CI indexed document',exact:true})).toBeVisible();
});
test('accessible worker selection, close and pause',async({page})=>{
  await page.goto('/');await page.locator('.scene-nodes button').nth(1).click();await expect(page.getByRole('heading',{name:'Shard 01',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Close shard inspection',exact:true}).click();await expect(page.getByRole('heading',{name:'Cluster pulse',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Pause 3D animation',exact:true}).click();await expect(page.getByRole('button',{name:'Resume 3D animation',exact:true})).toBeVisible();
});
for(const width of [1440,1280,1024,768,390,320])test(`both screens fit ${width}px`,async({page})=>{
  await page.setViewportSize({width,height:1000});await page.goto('/');
  for(const name of ['Search explorer','Document studio']){
    await page.getByRole('button',{name,exact:true}).click();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  }
});
