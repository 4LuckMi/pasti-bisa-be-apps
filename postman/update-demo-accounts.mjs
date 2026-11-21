import fs from 'node:fs';
import {fileURLToPath} from 'node:url';
const file=fileURLToPath(new URL('./PastiSiap-Backend.postman_collection.json',import.meta.url));
const collection=JSON.parse(fs.readFileSync(file,'utf8').replace(/^\uFEFF/,''));
for(const key of ['adminEmail','adminPassword','staffEmail','staffPassword']) {
  if(!collection.variable.some(item=>item.key===key))collection.variable.push({key,value:key==='adminEmail'?'admin.demo@pastisiap.test':key==='staffEmail'?'staff.demo@pastisiap.test':'',type:'string'});
}
const folder={name:'Local demo - admin and question staff login',description:'Jalankan scripts/setup-demo-accounts.ps1 lalu import environment PRIVATE dari target/PastiSiap-Demo-Accounts.postman_environment.json. Password acak hanya ada di file lokal tersebut. Pilih satu role, ambil CSRF, lalu login. Cookie session Postman dipakai oleh folder API internal. Login role lain mengganti session yang sedang digunakan.',item:[]};
folder.item.push({name:'Get CSRF before demo login',request:{method:'GET',header:[],url:'{{baseUrl}}/api/v1/csrf'},event:[{listen:'test',script:{type:'text/javascript',exec:["pm.test('HTTP 200',()=>pm.response.to.have.status(200));","if(pm.response.code===200) pm.collectionVariables.set('csrfToken',pm.response.json().token);"]}}]});
for(const [kind,role]of [['admin','PLATFORM_ADMIN'],['staff','QUESTION_STAFF']])folder.item.push({
  name:`Login demo ${kind} (${role})`,
  request:{method:'POST',header:[{key:'Content-Type',value:'application/json'},{key:'X-CSRF-TOKEN',value:'{{csrfToken}}'}],url:'{{baseUrl}}/api/v1/auth/login',body:{mode:'raw',raw:JSON.stringify({email:`{{${kind}Email}}`,password:`{{${kind}Password}}`},null,2),options:{raw:{language:'json'}}},description:'Import private local environment first. Uses ordinary password login; no privileged public bootstrap endpoint.'},
  event:[{listen:'test',script:{type:'text/javascript',exec:["pm.test('HTTP 200',()=>pm.response.to.have.status(200));",`if(pm.response.code===200){ const user=pm.response.json().user; pm.test('Exact internal role',()=>{pm.expect(user.userType).to.eql('INTERNAL');pm.expect(user.roles).to.eql(['${role}']);}); }`,"// Login rotates the CSRF token: fetch GET /csrf again before the next mutation."]}}]
});
const index=collection.item.findIndex(item=>item.name===folder.name);
if(index>=0)collection.item[index]=folder;else collection.item.push(folder);
fs.writeFileSync(file,JSON.stringify(collection,null,2)+'\n');
console.log('Updated local demo login folder (no passwords exported).');
