/** Development-only instrumentation. No telemetry leaves the local browser. */
if(import.meta.env.DEV) {
  const observed:{lcpMs:number|null;cls:number;maxInteractionMs:number|null}={lcpMs:null,cls:0,maxInteractionMs:null};
  const observe=(type:string,callback:(entry:PerformanceEntry)=>void)=>{
    try{new PerformanceObserver(list=>list.getEntries().forEach(callback)).observe({type,buffered:true,durationThreshold:16} as PerformanceObserverInit);}catch{/* Unsupported browser entry types are reported as unavailable. */}
  };
  observe('largest-contentful-paint',entry=>{observed.lcpMs=entry.startTime;});
  observe('layout-shift',entry=>{const shift=entry as PerformanceEntry&{value:number;hadRecentInput:boolean};if(!shift.hadRecentInput)observed.cls+=shift.value;});
  observe('event',entry=>{const event=entry as PerformanceEntry&{interactionId?:number};if(event.interactionId)observed.maxInteractionMs=Math.max(observed.maxInteractionMs??0,event.duration);});
  let timer:ReturnType<typeof setTimeout>;
  const report=()=>{
    clearTimeout(timer);timer=setTimeout(async()=>{
      const axe=(await import('axe-core')).default;
      const result=await axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa','wcag22aa']}});
      console.info('SFX_AUDIT '+JSON.stringify({capturedAt:new Date().toISOString(),design:getComputedStyle(document.documentElement).getPropertyValue('--design-revision').trim()||'baseline',
        viewport:innerWidth,route:location.pathname,performance:{...observed,note:'Local dev observation; max interaction duration is not field INP'},
        violations:result.violations.map(item=>({id:item.id,impact:item.impact,description:item.description,nodes:item.nodes.map(node=>({target:node.target,summary:node.failureSummary}))})),
        incomplete:result.incomplete.map(item=>({id:item.id,nodes:item.nodes.map(node=>({target:node.target,summary:node.failureSummary}))})),passRules:result.passes.length}));
    },800);
  };
  addEventListener('load',report);addEventListener('pointerup',report);addEventListener('keyup',report);addEventListener('resize',report);
  setTimeout(report,1200);
}
export {};
