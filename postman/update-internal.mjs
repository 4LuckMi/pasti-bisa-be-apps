// Update only the two generated internal folders; preserve existing user/student requests and variables.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const dir=path.dirname(fileURLToPath(import.meta.url));
const read=file=>JSON.parse(fs.readFileSync(file,'utf8').replace(/^\uFEFF/,''));
const file=path.join(dir,'PastiSiap-Backend.postman_collection.json');
const collection=read(file),inventory=read(path.join(dir,'../docs/backend/endpoint-inventory.json'));
const examples=read(path.join(dir,'../docs/backend/api-examples.json'));
const variables={managedUserId:'',categoryId:'',examTypeId:'',blueprintId:'',blueprintVersionId:'',expectedActiveVersionIdJson:'null',planId:'',planFamilyCode:'PREMIUM_1_MONTH',policyId:'',orderId:'',subscriptionId:'',jobId:'',questionVersionId:'',draftRevision:'0',refundKey:'',operationReason:'Pemeriksaan operasional pada lingkungan development'};
collection.variable??=[];for(const [key,value]of Object.entries(variables))if(!collection.variable.some(v=>v.key===key))collection.variable.push({key,value,type:'string'});
const variableByPath=path=>path.startsWith('/api/v1/admin/users/')?'managedUserId':path.startsWith('/api/v1/admin/plans/')?'planId':path.startsWith('/api/v1/admin/blueprints/')?'blueprintId':path.startsWith('/api/v1/admin/orders/')?'orderId':path.startsWith('/api/v1/admin/subscriptions/')?'subscriptionId':path.startsWith('/api/v1/admin/jobs/')?'jobId':path.includes('/categories/')?'categoryId':path.includes('/exam-types/')?'examTypeId':path.includes('/subtests/')?'subtestId':path.includes('/topics/')?'topicId':path.includes('/imports/')?'batchId':'questionId';
function requestBody(ep){
 let body=structuredClone(examples[ep.request]);if(!body)return undefined;
 const ids={categoryId:'categoryId',examTypeId:'examTypeId',subtestId:'subtestId',topicId:'topicId',scoringPolicyId:'policyId',expectedVersionId:'questionVersionId'};
 const walk=value=>{if(!value||typeof value!=='object')return;for(const [key,item]of Object.entries(value)){if(key==='reason')value[key]='{{operationReason}}';else if(ids[key])value[key]='{{'+ids[key]+'}}';else if(key==='questionSetIds')value[key]=['{{questionSetId}}'];else if(key==='expectedActiveVersionId')value[key]='__ACTIVE_VERSION_JSON__';else if(key==='expectedRevision')value[key]='__DRAFT_REVISION__';else if(key==='candidateIds')value[key]=['{{candidateId}}'];else walk(item);}};walk(body);
 if(ep.request==='PlanRequest')body.code='{{planFamilyCode}}';
 return {mode:'raw',raw:JSON.stringify(body,null,2).replace('"__ACTIVE_VERSION_JSON__"','{{expectedActiveVersionIdJson}}').replace('"__DRAFT_REVISION__"','{{draftRevision}}'),options:{raw:{language:'json'}}};
}
function entry(ep){
 let url='{{baseUrl}}'+ep.path.replace('{id}','{{'+variableByPath(ep.path)+'}}').replace('{versionId}',ep.path.includes('/blueprints/')?'{{blueprintVersionId}}':'{{questionVersionId}}');
 if(ep.method==='get'&&ep.path.match(/\/admin\/users\/\{id\}(\/attempts)?$/))url+='?reason=Support%20development%20verification';
 else if(ep.method==='get'&&examples[ep.response]?.items)url+='?offset=0&limit=10';
 const mutation=['post','put','patch','delete'].includes(ep.method);
 const request={method:ep.method.toUpperCase(),header:[...(mutation?[{key:'X-CSRF-TOKEN',value:'{{csrfToken}}',type:'text'}]:[]),...(ep.request?[{key:'Content-Type',value:'application/json',type:'text'}]:[]),...(ep.idempotency?[{key:'Idempotency-Key',value:'{{refundKey}}',type:'text'}]:[])],url,
  description:ep.description+'\n\nRole: '+(ep.auth==='admin'?'PLATFORM_ADMIN':'QUESTION_STAFF')+'. Login menggunakan folder Identity, lalu ambil CSRF dari cookie yang sama. '+(ep.auth==='admin'?'managedUserId harus dipilih eksplisit; tidak mengambil ID admin yang sedang login. ':'')+(ep.notes??[]).join('\n')};
 if(ep.request)request.body=requestBody(ep);
 const scripts=[`pm.test('HTTP ${ep.status}', function () { pm.response.to.have.status(${ep.status}); });`,"pm.test('Request correlation ID', function () { pm.expect(pm.response.headers.get('X-Request-ID')).to.be.ok; });"];
 const save=(condition,key,source='data.id')=>{if(condition)scripts.push(`if (pm.response.code < 300) { const data = pm.response.json(); if (${source}) pm.collectionVariables.set('${key}', ${source}); }`);};
 save(ep.method==='post'&&ep.path==='/api/v1/admin/users','managedUserId');
 save(ep.method==='post'&&ep.path==='/api/v1/admin/categories','categoryId');
 save(ep.method==='post'&&ep.path==='/api/v1/admin/exam-types','examTypeId');
 save(ep.method==='post'&&ep.path==='/api/v1/admin/subtests','subtestId');
 save(ep.method==='post'&&ep.path==='/api/v1/admin/topics','topicId');
 save(ep.method==='post'&&/^\/api\/v1\/admin\/plans(\/\{id\}\/versions)?$/.test(ep.path),'planId');
 save(ep.method==='post'&&ep.path==='/api/v1/admin/scoring-policies','policyId');
 save(ep.method==='post'&&ep.path==='/api/v1/admin/blueprints','blueprintId');
 save(ep.method==='post'&&ep.path==='/api/v1/admin/blueprints/{id}/versions','blueprintVersionId');
 if(ep.path.endsWith('/publish'))scripts.push("if (pm.response.code === 200) { const data = pm.response.json(); pm.collectionVariables.set('expectedActiveVersionIdJson', JSON.stringify(data.blueprint.activeVersionId)); }");
 if(ep.path.endsWith('/draft'))scripts.push("if (pm.response.code === 200) { const data = pm.response.json(); pm.collectionVariables.set('questionVersionId', data.question.versionId); pm.collectionVariables.set('draftRevision', String(data.revision)); }");
 const event=[{listen:'test',script:{type:'text/javascript',exec:scripts}}];
 if(ep.idempotency)event.unshift({listen:'prerequest',script:{type:'text/javascript',exec:["if (!pm.collectionVariables.get('refundKey')) pm.collectionVariables.set('refundKey', pm.variables.replaceIn('{{$guid}}')); "]}});
 const sample=ep.example??(ep.response?(ep.array?[examples[ep.response]]:examples[ep.response]):undefined);
 return {name:request.method+' '+ep.title,request,event,response:sample===undefined?[]:[{name:'Synthetic example - '+ep.status,originalRequest:request,status:ep.status===503?'Service Unavailable':ep.status===202?'Accepted':ep.status===201?'Created':'OK',code:ep.status,_postman_previewlanguage:'json',header:[{key:'Content-Type',value:'application/json'}],cookie:[],body:JSON.stringify(sample,null,2)}]};
}
for(const [group,name]of [['Petugas soal lanjutan','Question staff - complete APIs'],['Admin platform','Platform admin - complete APIs']]){
 const folder={name,description:'Generated from the reviewed API inventory. Pilih akun dan target operasi secara eksplisit. Mutasi beralasan dan CSRF wajib; jangan memakai Run Folder untuk perubahan yang belum ditinjau.',item:inventory.filter(ep=>ep.group===group).map(entry)};
 const current=collection.item.findIndex(item=>item.name===name);if(current>=0)collection.item[current]=folder;else collection.item.push(folder);
}
collection.info.name='PastiSiap Backend - Student, Question Staff and Admin APIs';
fs.writeFileSync(file,JSON.stringify(collection,null,2)+'\n');
console.log('Updated internal folders:',collection.item.filter(x=>/complete APIs/.test(x.name)).map(x=>x.name+' '+x.item.length).join(', '));
