import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const dir=path.dirname(fileURLToPath(import.meta.url));
const read=file=>JSON.parse(fs.readFileSync(file,'utf8').replace(/^\uFEFF/,''));
const filename=path.join(dir,'PastiSiap-Backend.postman_collection.json');
const collection=read(filename),inventory=read(path.join(dir,'../docs/backend/endpoint-inventory.json'));
const examples=read(path.join(dir,'../docs/backend/api-examples.json'));
collection.variable??=[];
for(const [key,value]of Object.entries({cohortId:'',planId:'',historyOffset:'0',pageLimit:'12'}))
 if(!collection.variable.some(v=>v.key===key))collection.variable.push({key,value,type:'string'});
function entry(ep){
 let url='{{baseUrl}}'+ep.path.replace('{id}',ep.path.startsWith('/api/v1/plans/')?'{{planId}}':'{{cohortId}}');
 if(ep.path.endsWith('/dashboard'))url+='?category={{category}}&mode=TRYOUT';
 if(ep.path.endsWith('/learning-history'))url+='?category={{category}}&period=ALL&limit={{pageLimit}}&offset={{historyOffset}}';
 if(ep.path.includes('/catalog/')||ep.path.endsWith('/cohorts'))url+='?category={{category}}&limit={{pageLimit}}&offset=0';
 if(ep.path==='/api/v1/plans')url+='?limit={{pageLimit}}&offset=0';
 if(ep.path.endsWith('/entries'))url+='?limit={{pageLimit}}&offset=0';
 const request={method:ep.method.toUpperCase(),header:[],url,description:ep.description+'\n\nMemerlukan session CUSTOMER. Jalankan Get CSRF Token dan login/register dahulu. Jangan Run Folder untuk mutasi privacy tanpa meninjau pilihan persetujuan.'};
 if(ep.request){request.header.push({key:'Content-Type',value:'application/json'},{key:'X-CSRF-TOKEN',value:'{{csrfToken}}'});
  request.body={mode:'raw',raw:JSON.stringify({leaderboardOptIn:false},null,2),options:{raw:{language:'json'}}};}
 const lines=['pm.test("Status 200", () => pm.response.to.have.status(200));',
  'pm.test("Request ID tersedia", () => pm.expect(pm.response.headers.get("X-Request-ID")).to.be.a("string"));'];
 if(ep.path.endsWith('/cohorts'))lines.push('const first = pm.response.json().items?.[0]; if (first) pm.collectionVariables.set("cohortId", first.id);');
 if(ep.path==='/api/v1/plans')lines.push('const first = pm.response.json().items?.[0]; if (first) pm.collectionVariables.set("planId", first.id);');
 if(ep.path.endsWith('/learning-history'))lines.push('pm.test("Pagination tersedia", () => { const p = pm.response.json(); pm.expect(p.items).to.be.an("array"); pm.expect(p.total).to.be.a("number"); });');
 const sample=examples[ep.response];
 return {name:request.method+' '+ep.title,request,event:[{listen:'test',script:{type:'text/javascript',exec:lines}}],response:[{name:'Synthetic example',originalRequest:request,status:'OK',code:200,_postman_previewlanguage:'json',header:[{key:'Content-Type',value:'application/json'}],cookie:[],body:JSON.stringify(sample,null,2)}]};
}
const name='Student screens - current contracts';
const folder={name,description:'API backend untuk FE terbaru. Endpoint lama tetap tersedia. Pilih cohortId dari daftar cohort sebelum membaca peringkat. Collection tidak membuat pembayaran atau ranking palsu.',item:inventory.filter(e=>e.group==='Pengalaman siswa terbaru').map(entry)};
const index=collection.item.findIndex(x=>x.name===name);if(index<0)collection.item.push(folder);else collection.item[index]=folder;
fs.writeFileSync(filename,JSON.stringify(collection,null,2)+'\n');
console.log('Updated student screens:',folder.item.length,'requests');
