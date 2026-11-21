import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const dir=path.dirname(fileURLToPath(import.meta.url)),file=path.join(dir,'PastiSiap-Backend.postman_collection.json');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
const collection=read(file),inventory=read(path.join(dir,'../docs/backend/endpoint-inventory.json')),examples=read(path.join(dir,'../docs/backend/api-examples.json'));
const variables={questionSetId:'',questionSetCode:'UTBK_2025_A',sourceYear:'2025',workbookBatchId:'',workbookCandidateId:'',workbookSourceItemCode:'Q001',workbookTopicIdJson:'null',workbookSheetMappings:'[]',workbookCandidateIdsJson:'[]',workbookSavedQuestionIdsJson:'[]'};
collection.variable??=[];for(const [key,value]of Object.entries(variables))if(!collection.variable.some(v=>v.key===key))collection.variable.push({key,value,type:'string'});
function entry(ep){
 const binary=ep.binary,source=ep.path.includes('/question-sets');
 let url='{{baseUrl}}'+ep.path.replace('{id}',source?'{{questionSetId}}':'{{workbookBatchId}}').replace('{candidateId}','{{workbookCandidateId}}');
 if(ep.path==='/api/v1/staff/question-sets'||ep.path==='/api/v1/admin/question-sets')url+='?examTypeId={{examTypeId}}&limit=20&offset=0';
 if(ep.method==='get'&&ep.path==='/api/v1/staff/workbook-imports')url+='?questionSetId={{questionSetId}}&limit=20&offset=0';
 if(ep.path.endsWith('/candidates')||ep.path.endsWith('/items')&&ep.method==='get')url+='?offset=0&limit=20';
 const request={method:ep.method.toUpperCase(),header:[],url,description:ep.description+'\n\nRole: '+(ep.auth==='admin'?'PLATFORM_ADMIN':'QUESTION_STAFF')+'. Login dan CSRF memakai cookie session yang sama. Jalankan bertahap; tinjau mutasi sebelum mengirim. Unduhan source file memakai GET /staff/imports/{{workbookBatchId}}/source.'};
 if(['post','patch'].includes(ep.method))request.header.push({key:'X-CSRF-TOKEN',value:'{{csrfToken}}'});
 if(ep.multipart){
  request.body={mode:'formdata',formdata:[{key:'metadata',type:'text',contentType:'application/json',value:'{"questionSetId":"{{questionSetId}}","sheets":{{workbookSheetMappings}}}'},{key:'file',type:'file',src:[],description:'Pilih file .xlsx yang sudah diisi dari template.'}]};
 }else if(ep.request){
  const body=structuredClone(examples[ep.request]);if(body.reason)body.reason='{{operationReason}}';
  if(ep.request==='SourceRequest'){body.examTypeId='{{examTypeId}}';body.code='{{questionSetCode}}';body.sourceYear='__SOURCE_YEAR__';}
  if(ep.request==='SaveWorkbookCandidates')body.candidateIds='__CANDIDATE_IDS__';
  if(ep.request==='LinkSourceQuestion'){body.questionId='{{questionId}}';body.sourceItemCode='{{workbookSourceItemCode}}';}
  if(ep.request==='CandidateCorrection'){body.sourceItemCode='{{workbookSourceItemCode}}';body.topicId='__TOPIC__';}
  request.header.push({key:'Content-Type',value:'application/json'});
  request.body={mode:'raw',raw:JSON.stringify(body,null,2).replace('"__SOURCE_YEAR__"','{{sourceYear}}').replace('"__CANDIDATE_IDS__"','{{workbookCandidateIdsJson}}').replace('"__TOPIC__"','{{workbookTopicIdJson}}'),options:{raw:{language:'json'}}};
 }
 const scripts=[`pm.test('HTTP ${ep.status}', () => pm.response.to.have.status(${ep.status}));`];
 if(ep.method==='post'&&ep.path==='/api/v1/staff/question-sets')scripts.push("if (pm.response.code === 201) pm.collectionVariables.set('questionSetId', pm.response.json().id);");
 if(ep.method==='post'&&ep.path==='/api/v1/staff/workbook-imports')scripts.push("if (pm.response.code === 202) pm.collectionVariables.set('workbookBatchId', pm.response.json().id);");
 if(ep.path.endsWith('/template-mapping'))scripts.push("if (pm.response.code === 200) pm.collectionVariables.set('workbookSheetMappings', JSON.stringify(pm.response.json()));");
 if(ep.path.endsWith('/candidates'))scripts.push("if (pm.response.code === 200) { const rows=pm.response.json().items; const first=rows[0]; if(first){pm.collectionVariables.set('workbookCandidateId',first.id);pm.collectionVariables.set('workbookSourceItemCode',first.sourceItemCode);pm.collectionVariables.set('workbookTopicIdJson',JSON.stringify(first.topicId));} pm.collectionVariables.set('workbookCandidateIdsJson',JSON.stringify(rows.filter(r=>!r.validationErrors.length&&!r.resolvedVersionId).map(r=>r.id))); }");
 if(ep.path.endsWith('/save'))scripts.push("if (pm.response.code === 200) {const rows=pm.response.json(); pm.collectionVariables.set('workbookSavedQuestionIdsJson',JSON.stringify(rows.map(q=>q.id)));if(rows[0]){pm.collectionVariables.set('questionId',rows[0].id);pm.collectionVariables.set('questionVersionId',rows[0].versionId);}} ");
 const sample=ep.response?(ep.array?[examples[ep.response]]:examples[ep.response]):undefined;
 return {name:request.method+' '+ep.title,request,event:[{listen:'test',script:{type:'text/javascript',exec:scripts}}],response:binary||sample===undefined?[]:[{name:'Synthetic example',originalRequest:request,status:ep.status===201?'Created':ep.status===202?'Accepted':'OK',code:ep.status,_postman_previewlanguage:'json',header:[{key:'Content-Type',value:'application/json'}],cookie:[],body:JSON.stringify(sample,null,2)}]};
}
const name='Question sources and Excel import';
const folder={name,description:'Urutan: buat sumber → mapping/template → upload → tunggu worker → preview/koreksi → save DRAFT → publish per master → admin mengatur sumber/difficulty pada blueprint baru. Jangan Run Folder: archive dan koreksi harus dipilih eksplisit.',item:inventory.filter(e=>e.group==='Sumber soal dan Excel').map(entry)};
const current=collection.item.findIndex(x=>x.name===name);if(current<0)collection.item.push(folder);else collection.item[current]=folder;
fs.writeFileSync(file,JSON.stringify(collection,null,2)+'\n');console.log('Updated workbook folder:',folder.item.length,'requests');
