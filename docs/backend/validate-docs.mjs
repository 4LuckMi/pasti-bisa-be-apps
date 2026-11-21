import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
const dir=path.dirname(fileURLToPath(import.meta.url));
const read=name=>JSON.parse(fs.readFileSync(path.join(dir,name),'utf8').replace(/^\uFEFF/,''));
const spec=read('openapi.json'),raw=read('openapi-runtime.json'),inventory=read('endpoint-inventory.json'),examples=read('api-examples.json'),db=read('database-schema.json');
const methods=['get','post','put','patch','delete'];
const operations=doc=>Object.entries(doc.paths).flatMap(([url,ops])=>methods.filter(m=>ops[m]).map(m=>m.toUpperCase()+' '+url));
const schemaRef=value=>value.$ref?.startsWith('#/components/schemas/')?spec.components.schemas[value.$ref.split('/').at(-1)]:null;
for(const key of operations(raw))assert(inventory.some(x=>x.method.toUpperCase()+' '+x.path===key),'Undocumented runtime '+key);
assert.equal(new Set(operations(spec)).size,inventory.length);
assert.equal(new Set(db.columns.map(c=>c.table_name)).size,41);
assert(!operations(spec).some(x=>/\/me(?:\/|$)/.test(x)),'Legacy URL in docs');
function walk(value){if(!value||typeof value!=='object')return;if(value.$ref){assert(value.$ref.startsWith('#/'),'External unresolved ref');const parts=value.$ref.slice(2).split('/');let found=spec;for(const p of parts)found=found?.[p];assert(found,'Unresolved '+value.$ref);}for(const child of Object.values(value))walk(child);}
walk(spec);
// Validate keys/types/required/bounds of all synthetic examples against reviewed schemas.
function check(value,schema,where){
 const resolved=schemaRef(schema);if(resolved)return check(value,resolved,where);
 if(schema.anyOf){assert(schema.anyOf.some(s=>{try{check(value,s,where);return true;}catch{return false;}}),'No anyOf match '+where);return;}
 const types=Array.isArray(schema.type)?schema.type:[schema.type??'object'];
 if(value===null){assert(types.includes('null'),where+' unexpectedly null');return;}
 if(types.includes('array')){assert(Array.isArray(value),where+' expected array');if(schema.minItems!==undefined)assert(value.length>=schema.minItems,where+' minItems');if(schema.maxItems!==undefined)assert(value.length<=schema.maxItems,where+' maxItems');value.forEach((v,i)=>check(v,schema.items??{},where+'['+i+']'));return;}
 if(types.includes('object')){assert(typeof value==='object'&&!Array.isArray(value),where+' expected object');for(const p of schema.required??[])assert(Object.hasOwn(value,p),where+' missing '+p);for(const [k,v]of Object.entries(value)){if(schema.properties?.[k])check(v,schema.properties[k],where+'.'+k);else if(schema.additionalProperties&&typeof schema.additionalProperties==='object')check(v,schema.additionalProperties,where+'.'+k);else if(schema.properties)assert(false,where+' undocumented field '+k);}return;}
 if(types.includes('string')){assert.equal(typeof value,'string',where);if(schema.minLength!==undefined)assert(value.length>=schema.minLength,where+' minLength');if(schema.maxLength!==undefined)assert(value.length<=schema.maxLength,where+' maxLength');if(schema.pattern)assert(new RegExp(schema.pattern).test(value),where+' pattern');if(schema.format==='uuid')assert(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value),where+' UUID');if(schema.format==='date-time')assert(Number.isFinite(Date.parse(value)),where+' timestamp');}
 else if(types.includes('boolean'))assert.equal(typeof value,'boolean',where);
 else if(types.includes('number')||types.includes('integer')){assert.equal(typeof value,'number',where);if(types.includes('integer'))assert(Number.isInteger(value),where+' integer');if(schema.minimum!==undefined)assert(value>=schema.minimum,where+' minimum');if(schema.maximum!==undefined)assert(value<=schema.maximum,where+' maximum');}
 if(schema.enum)assert(schema.enum.includes(value),where+' enum');
}
for(const [name,value]of Object.entries(examples)){assert(spec.components.schemas[name],'Example without schema '+name);check(value,spec.components.schemas[name],name);}
for(const ep of inventory){const op=spec.paths[ep.path][ep.method];for(const [status,res]of Object.entries(op.responses))for(const [mime,body]of Object.entries(res.content??{}))if(body.example!==undefined)check(body.example,body.schema,ep.path+' '+status+' '+mime);}
const slug=text=>text.replace(/<[^>]+>/g,'').replace(/[`*]/g,'').trim().toLowerCase().replace(/[^\p{L}\p{N}_\s-]/gu,'').replace(/\s/g,'-');
const markdown=fs.readdirSync(dir).filter(f=>f.endsWith('.md'));
const anchors=new Map();
for(const name of markdown){const text=fs.readFileSync(path.join(dir,name),'utf8');const ids=new Set([...text.matchAll(/<a id="([^"]+)"/g)].map(x=>x[1]));for(const h of text.matchAll(/^#{1,6}\s+(.+)$/gm))ids.add(slug(h[1]));anchors.set(name,ids);assert.equal((text.match(/^```/gm)??[]).length%2,0,'Unclosed fence '+name);for(const block of text.matchAll(/```json\s*\n([\s\S]*?)\n```/g))JSON.parse(block[1]);}
let linkCount=0,diagramCount=0;
for(const name of markdown){const text=fs.readFileSync(path.join(dir,name),'utf8');for(const match of text.matchAll(/\]\(([^\s)]+)\)/g)){const href=match[1];if(/^(https?:|mailto:)/.test(href))continue;const [filename,anchor]=href.split('#');const target=filename?path.resolve(dir,filename):path.join(dir,name);assert(fs.existsSync(target),'Broken link '+name+' -> '+href);if(anchor&&target.endsWith('.md')&&path.dirname(target)===dir)assert(anchors.get(path.basename(target))?.has(anchor),'Broken anchor '+name+' -> '+href);linkCount++;}
 for(const block of text.matchAll(/```mermaid\s*\n([\s\S]*?)\n```/g)){assert(/^(flowchart|sequenceDiagram|erDiagram|stateDiagram-v2)\b/.test(block[1].trim()),'Unknown diagram '+name);assert(!block[1].includes('\\n'),'Escaped newline in diagram');diagramCount++;}}
const postman=JSON.parse(fs.readFileSync(path.resolve(dir,'../../postman/PastiSiap-Backend.postman_collection.json'),'utf8').replace(/^\uFEFF/,''));
function visit(items){for(const item of items){if(item.item)visit(item.item);else if(item.request){const url=typeof item.request.url==='string'?item.request.url:item.request.url.raw;const pathname=url.replace('{{baseUrl}}','').split('?')[0];if(!pathname.startsWith('/'))continue;const normalized=pathname.replace(/\{\{[^}]+\}\}/g,'{}');assert(inventory.some(e=>e.method.toUpperCase()===item.request.method&&e.path.replace(/\{[^}]+\}/g,'{}')===normalized),'Postman request undocumented '+item.request.method+' '+pathname);}}}
visit(postman.item);
console.log(`PASS: ${inventory.length} operations, ${Object.keys(examples).length} example DTOs, ${linkCount} local links/anchors, ${diagramCount} diagram sources, ${db.columns.length} columns; all runtime and Postman operations covered.`);
