-- Read-only schema metadata. Does not select account, order, session or credential rows.
SELECT jsonb_build_object(
  'schema','public',
  'columns',(SELECT jsonb_agg(to_jsonb(c) ORDER BY table_name,ordinal_position) FROM (
    SELECT table_name,column_name,ordinal_position,column_default,is_nullable,data_type,character_maximum_length,numeric_precision,numeric_scale,udt_name
    FROM information_schema.columns WHERE table_schema='public' AND table_name<>'flyway_schema_history') c),
  'constraints',(SELECT jsonb_agg(jsonb_build_object('table_name',r.relname,'name',c.conname,'type',c.contype,'definition',pg_get_constraintdef(c.oid)) ORDER BY r.relname,c.conname)
    FROM pg_constraint c JOIN pg_class r ON r.oid=c.conrelid JOIN pg_namespace n ON n.oid=r.relnamespace WHERE n.nspname='public' AND r.relname<>'flyway_schema_history'),
  'indexes',(SELECT jsonb_agg(to_jsonb(i) ORDER BY tablename,indexname) FROM pg_indexes i WHERE schemaname='public' AND tablename<>'flyway_schema_history'),
  'triggers',(SELECT jsonb_agg(jsonb_build_object('table_name',r.relname,'name',t.tgname,'definition',pg_get_triggerdef(t.oid)) ORDER BY r.relname,t.tgname)
    FROM pg_trigger t JOIN pg_class r ON r.oid=t.tgrelid JOIN pg_namespace n ON n.oid=r.relnamespace WHERE n.nspname='public' AND NOT t.tgisinternal)
);
