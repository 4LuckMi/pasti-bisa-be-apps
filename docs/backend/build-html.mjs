// Optional artifact renderer. App runtime does not depend on these temporary tooling packages.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {createRequire} from 'node:module';
const dir=path.dirname(fileURLToPath(import.meta.url));
const renderRoot=process.argv[2]??path.join(os.tmpdir(),'pastisiap-doc-renderer');
const modules=path.join(renderRoot,'node_modules');
const {Marked}=await import(pathToFileURL(path.join(modules,'marked/lib/marked.esm.js')).href);
const rendererRequire=createRequire(path.join(renderRoot,'package.json'));
const puppeteer=await import(pathToFileURL(rendererRequire.resolve('puppeteer-core')).href).then(m=>m.default??m);
const chrome=process.argv[3]??['C:/Program Files/Google/Chrome/Application/chrome.exe','C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(p=>fs.existsSync(p));
if(!chrome)throw Error('Provide a headless Chrome/Edge executable as second argument.');
const files=['README.md','01-architecture.md','02-database.md','03-api-reference.md','data-contracts.md','database-dictionary.md','04-roadmap.md','05-operations.md'];
const labels=['Mulai di sini','Arsitektur dan flowchart','Model data dan ERD','46 endpoint API','Kamus DTO dan JSON','40 tabel database','Status dan roadmap','Menjalankan dan troubleshooting'];
const escape=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const slug=s=>s.replace(/<[^>]+>/g,'').replace(/[`*]/g,'').trim().toLowerCase().replace(/[^\p{L}\p{N}_\s-]/gu,'').replace(/\s/g,'-');
const chapter=name=>'c-'+name.replace('.md','').toLowerCase();
const diagrams=[];
for(const name of files){const text=fs.readFileSync(path.join(dir,name),'utf8');for(const match of text.matchAll(/```mermaid\s*\n([\s\S]*?)\n```/g)){const heading=[...text.slice(0,match.index).matchAll(/^#{1,4}\s+(.+)$/gm)].at(-1)?.[1]??'Diagram';diagrams.push({id:'diagram-'+String(diagrams.length+1).padStart(2,'0'),file:name,title:heading,source:match[1]});}}
// Fresh isolated browser profile; never uses the user's normal browser/session.
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'pastisiap-documentation-'));
const profile=path.join(tmp,'profile');
const payload=JSON.stringify(diagrams).replaceAll('<','\\u003c');
const moduleUrl=pathToFileURL(path.join(modules,'mermaid/dist/mermaid.esm.min.mjs')).href;
const renderer=`<!doctype html><meta charset="utf-8"><div id="result"></div><script type="module">
import mermaid from ${JSON.stringify(moduleUrl)};
mermaid.initialize({startOnLoad:false,securityLevel:'strict',theme:'base',fontFamily:'Arial, sans-serif',themeVariables:{primaryColor:'#edf5ff',primaryBorderColor:'#2873ad',primaryTextColor:'#17334c',lineColor:'#497394',secondaryColor:'#fff1dd',tertiaryColor:'#edf8f5',fontSize:'16px'},flowchart:{useMaxWidth:false,htmlLabels:false},sequence:{useMaxWidth:false},er:{useMaxWidth:false}});
const results=[];
for(const d of ${payload}){try{const result=await mermaid.render(d.id,d.source);results.push({...d,svg:result.svg});}catch(error){results.push({...d,error:String(error)});}}
document.getElementById('result').setAttribute('data-payload',btoa(unescape(encodeURIComponent(JSON.stringify(results)))));
</script>`;
fs.writeFileSync(path.join(tmp,'render.html'),renderer);
const browser=await puppeteer.launch({executablePath:chrome,headless:true,userDataDir:profile,args:['--disable-gpu','--disable-background-networking','--disable-default-apps','--no-first-run','--no-default-browser-check','--allow-file-access-from-files']});
process.on('exit',()=>{browser.process()?.kill();});
const page=await browser.newPage();
await page.setViewport({width:1600,height:1100,deviceScaleFactor:1});
page.on('pageerror',error=>console.error('Render error:',error.message));
await page.goto(pathToFileURL(path.join(tmp,'render.html')).href,{waitUntil:'networkidle0',timeout:60000});
await page.waitForFunction(()=>document.getElementById('result')?.hasAttribute('data-payload'),{timeout:60000});
const encoded=await page.$eval('#result',node=>node.getAttribute('data-payload'));
const output=JSON.parse(Buffer.from(encoded,'base64').toString('utf8'));
const failures=output.filter(d=>d.error);if(failures.length)throw Error(JSON.stringify(failures.map(({id,title,error})=>({id,title,error})),null,2));
fs.mkdirSync(path.join(dir,'diagrams'),{recursive:true});
for(const d of output)fs.writeFileSync(path.join(dir,'diagrams',d.id+'.svg'),d.svg);
fs.writeFileSync(path.join(dir,'diagrams','manifest.json'),JSON.stringify(output.map(({svg,...rest})=>rest),null,2)+'\n');
const articles=[];let diagramIndex=0;
for(const [i,name]of files.entries()){
 const prefix=chapter(name),md=fs.readFileSync(path.join(dir,name),'utf8');
 const marked=new Marked({gfm:true,breaks:false,renderer:{
  heading(token){return `<h${token.depth} id="${slug(token.text)}">${this.parser.parseInline(token.tokens)}</h${token.depth}>`;},
  code(token){if(token.lang==='mermaid'){const d=output[diagramIndex++];return `<figure class="diagram" data-figure="${d.id}"><div class="diagram-toolbar"><button onclick="diagramSize(this,false)">Sesuaikan lebar</button><button onclick="diagramSize(this,true)">Ukuran asli</button></div><div data-svg="${d.id}"></div><figcaption>${escape(d.title)}</figcaption><details><summary>Lihat source Mermaid</summary><pre><code>${escape(token.text)}</code></pre></details></figure>`;}return `<pre><code class="language-${escape(token.lang??'text')}">${escape(token.text)}</code></pre>`;}
 }});
 let html=marked.parse(md);
 const ids=new Set();html=html.replace(/ id="([^"]+)"/g,(_,value)=>{if(ids.has(value))return '';ids.add(value);return ` id="${prefix}-${value}"`;});
 html=html.replace(/href="([^"]+)"/g,(original,href)=>{if(/^(https?:|mailto:)/.test(href))return original;const [target,anchor]=href.split('#');if(target===''&&anchor)return `href="#${prefix}-${anchor}"`;if(files.includes(target))return `href="#${chapter(target)}${anchor?'-'+anchor:''}"`;return original;});
 html=html.replace(/<div data-svg="([^"]+)"><\/div>/g,(_,id)=>output.find(d=>d.id===id).svg);
 html=html.replace(/<table>/g,'<div class="table-scroll"><table>').replace(/<\/table>/g,'</table></div>');
 articles.push(`<section id="${prefix}" class="chapter"><div class="chapter-label">BAB ${i+1} / ${files.length}</div>${html}</section>`);
}
const css=`:root{--ink:#17334c;--muted:#60758a;--blue:#085fac;--line:#dce5ed;--paper:#fff;--canvas:#f3f6fa}*{box-sizing:border-box}html{scroll-behavior:smooth;scroll-padding-top:24px}body{margin:0;background:var(--canvas);color:var(--ink);font:15px/1.72 'Segoe UI',Arial,sans-serif}a{color:var(--blue);text-decoration:none}a:hover{text-decoration:underline}.layout{display:grid;grid-template-columns:250px minmax(0,1fr);max-width:1540px;margin:auto}.sidebar{position:sticky;top:0;align-self:start;height:100vh;overflow:auto;padding:32px 22px;background:#102c47;color:#fff}.brand{font-size:25px;font-weight:800;letter-spacing:-.6px}.brand span{color:#ff951f}.side-label{font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#9cb6cc;margin:6px 0 24px}.sidebar nav a{display:block;color:#e6eff7;font-size:14px;line-height:1.5;padding:10px 10px;border-radius:6px;margin:3px 0}.sidebar nav a:hover{background:#21445f;text-decoration:none}.sidebar p{font-size:12px;color:#b7c9d8}.print{margin-top:22px;border:1px solid #829db3;background:none;color:white;border-radius:6px;padding:10px 12px;cursor:pointer;font:inherit;font-size:13px}main{min-width:0;padding:36px 32px 80px}.hero{background:linear-gradient(135deg,#0b5795,#17334c);border-radius:14px;color:#fff;padding:32px;margin-bottom:24px}.eyebrow{font-size:12px;letter-spacing:1.6px;text-transform:uppercase;color:#cce0f2}.hero h1{font-size:32px;line-height:1.25;letter-spacing:-.5px;margin:12px 0}.hero p{max-width:780px;color:#e0ebf5;margin:10px 0}.metrics{display:flex;gap:10px;flex-wrap:wrap;margin-top:22px}.metrics span{background:#ffffff15;border:1px solid #ffffff30;border-radius:8px;padding:8px 12px;font-size:13px}.chapter{background:white;border:1px solid var(--line);border-radius:12px;padding:32px;margin:0 0 24px;min-width:0}.chapter-label{font-size:11px;letter-spacing:1.6px;font-weight:700;color:#72889b}.chapter h1{font-size:27px;line-height:1.3;letter-spacing:-.4px;margin:12px 0 18px}.chapter h2{font-size:22px;border-top:1px solid var(--line);padding-top:25px;margin-top:36px;line-height:1.4}.chapter h3{font-size:18px;margin:26px 0 12px;line-height:1.45}.chapter h4{font-size:15px;color:#3a596f;margin:22px 0 10px}p{margin:12px 0}li{margin:5px 0}code{font:12px/1.6 Consolas,'Cascadia Code',monospace;background:#edf2f7;padding:2px 4px;border-radius:3px;overflow-wrap:anywhere}pre{background:#f2f5f9;border:1px solid #dde5ed;border-radius:8px;padding:16px;overflow:auto;font-size:12px}pre code{padding:0;background:none;display:block;white-space:pre;overflow-wrap:normal}.table-scroll{overflow:auto;border:1px solid var(--line);border-radius:7px;margin:16px 0}table{border-collapse:collapse;width:100%;font-size:13px;min-width:560px}th{background:#edf3f8;color:#254b6b;text-align:left;font-weight:650}th,td{border-bottom:1px solid #e1e8ef;padding:10px 12px;vertical-align:top}tr:last-child td{border-bottom:0}td code{font-size:11.5px}.diagram{margin:24px 0;padding:20px 12px;border:1px solid #dbe7ef;border-radius:10px;background:#fbfdff;overflow:auto}.diagram svg{display:block;min-width:660px;max-width:none;width:100%;height:auto}.diagram figcaption{font-size:12px;text-align:center;color:var(--muted);margin-top:12px}.diagram details{font-size:12px;color:var(--muted);margin:10px 0 0}.diagram summary{cursor:pointer}blockquote{border-left:3px solid #e7a44c;margin:16px 0;padding:6px 18px;background:#fff8ef;color:#66523b}footer{color:var(--muted);font-size:12px;padding:0 8px}a[id]{scroll-margin-top:30px}@media(max-width:900px){.layout{grid-template-columns:1fr}.sidebar{position:relative;height:auto;padding:20px}.sidebar nav{display:flex;gap:4px;flex-wrap:wrap}.sidebar nav a{font-size:12px;padding:6px}.sidebar p,.print{display:none}.side-label{margin-bottom:10px}main{padding:18px 12px}.hero,.chapter{padding:22px 18px}.hero h1{font-size:27px}.chapter h1{font-size:24px}}@media print{body{background:white;font-size:10pt;color:#132a3f}.layout{display:block;max-width:none}.sidebar{display:none}main{padding:0}.hero{background:white;color:#17334c;border-bottom:3px solid #0b5795;border-radius:0;padding:0 0 20px}.hero p,.eyebrow{color:#4e687c}.metrics span{border-color:#ccd8e4;color:#17334c}.chapter{border:0;padding:0;break-before:page}.chapter-label{margin-top:12px}.table-scroll{overflow:visible;border-radius:0}table{min-width:0;font-size:8.5pt}th,td{padding:5px}pre{white-space:pre-wrap;overflow:visible;font-size:8pt}pre code{white-space:pre-wrap}.diagram{break-inside:avoid;overflow:visible;padding:6px}.diagram svg{min-width:0;max-width:100%;max-height:250mm;width:100%}.diagram details{display:none}h1,h2,h3,h4{break-after:avoid}a{color:inherit}footer{display:none}}`;
const nav=files.map((name,index)=>`<a href="#${chapter(name)}">${index+1}. ${labels[index]}</a>`).join('');
const html=`<!doctype html><html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Dokumentasi Backend PastiSiap</title><style>${css}</style></head><body><div class="layout"><aside class="sidebar"><div class="brand">Pasti<span>Siap</span></div><div class="side-label">Dokumentasi backend</div><nav>${nav}</nav><button class="print" onclick="window.print()">Cetak / Simpan PDF</button><p>Snapshot 19 November 2026<br>Gunakan Ctrl+F untuk mencari endpoint, DTO atau tabel.</p><p>Diagram dan halaman ini dapat dibaca offline. Contoh data seluruhnya sintetis.</p></aside><main><header class="hero"><div class="eyebrow">Backend engineering reference</div><h1>Arsitektur, kontrak API<br>dan struktur data PastiSiap</h1><p>Panduan untuk memahami implementasi yang sudah berjalan, menghubungkan frontend, menguji alur belajar, dan meninjau pekerjaan berikutnya.</p><div class="metrics"><span>46 operasi</span><span>37 skema DTO</span><span>40 tabel / 306 kolom</span><span>${output.length} diagram</span><span>Flyway V1–V6</span></div></header>${articles.join('\n')}<footer>Generated dari source dokumentasi dan snapshot runtime lokal. Tidak berisi kredensial atau data pengguna produksi.</footer></main></div></body></html>`;
const readabilityCss='.diagram svg{width:auto;max-width:100%;height:auto;min-width:0;margin:0 auto}.diagram-toolbar{display:flex;gap:8px;margin-bottom:14px}.diagram-toolbar button{font:12px Arial,sans-serif;color:#2f5877;border:1px solid #cedeea;background:white;border-radius:4px;padding:6px 10px;cursor:pointer}@media print{.diagram-toolbar{display:none}.diagram svg{width:100%;max-height:250mm}}';
const diagramScript='<script>function diagramSize(button,actual){const svg=button.closest("figure").querySelector("svg");svg.style.maxWidth=actual?"none":"100%";svg.style.width=actual?svg.viewBox.baseVal.width+"px":"auto";}</script>';
fs.writeFileSync(path.join(dir,'backend-documentation.html'),html.replace('</style>',readabilityCss+'</style>').replace('</body>',diagramScript+'</body>'));
const screenshot=path.join(tmp,'documentation-preview.png');
await page.goto(pathToFileURL(path.join(dir,'backend-documentation.html')).href,{waitUntil:'networkidle0'});
await page.screenshot({path:screenshot});
const diagramPreview=path.join(tmp,'diagram-preview.png');
const figures=await page.$$('figure.diagram');
await figures[0].screenshot({path:diagramPreview});
const erdPreview=path.join(tmp,'erd-preview.png');
await figures[8].screenshot({path:erdPreview});
const pageChecks=await page.evaluate(()=>{
 const ids=[...document.querySelectorAll('[id]')].map(node=>node.id),seen=new Set(),duplicates=[];
 ids.forEach(id=>{if(seen.has(id))duplicates.push(id);seen.add(id);});
 return {svgCount:document.querySelectorAll('figure.diagram svg').length,missingInternalLinks:[...document.querySelectorAll('a[href^="#"]')].map(a=>a.getAttribute('href').slice(1)).filter(id=>!document.getElementById(id)),duplicateIds:[...new Set(duplicates)],horizontalOverflow:document.documentElement.scrollWidth>innerWidth};
});
if(pageChecks.missingInternalLinks.length||pageChecks.horizontalOverflow)throw Error('HTML checks failed: '+JSON.stringify(pageChecks));
await browser.close();
console.log(JSON.stringify({html:'docs/backend/backend-documentation.html',diagrams:output.length,mermaidValidation:'all rendered',pageChecks,preview:screenshot,diagramPreview,erdPreview,rendererVersions:{mermaid:JSON.parse(fs.readFileSync(path.join(modules,'mermaid/package.json'),'utf8')).version,marked:JSON.parse(fs.readFileSync(path.join(modules,'marked/package.json'),'utf8')).version}},null,2));
