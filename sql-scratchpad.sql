use nepaccess_7_21;
SELECT * FROM nepaccess.eisdoc ed
	inner join `eis-meta` em on em.id = ed.id
    inner join document_text dt on dt.id = ed.id
    where ed.title like "%bay%";
    
    select document_type,count(1) 
    from nepaccess_7_21.eisdoc
    group by document_type
    order by 1 desc;
    
    select count(*) from eisdoc;
-- 17472
    select count(*) from eisdoc where process_id is null or process_id = "";   -- 753
    select count(*) from eisdoc where process_id is not null; 16719
    select count(*) from nepa_process; -- 5051
    
    select * from eisdoc where document_type = "NOI"
    
    
    select 753 + 16719
    
    
    
	select document_type,count(*)
    from eisdoc
    group by document_type
    order by 1 asc;
    
    select title,count(1) from nepaccess_7_21.document_text dt 
		inner join nepaccess_7_21.eisdoc ed on dt.document_id = ed.id
        where plaintext is not null
        group by title
        order by count(1);
    
    
        select title,id
        from nepaccess_7_21.eisdoc
		where id in (select document_id from nepaccess_7_21.document_text);
        
        use nepaccess_7_21;
        select * from application_user
		where role = "ADMIN"
        ;
    
    
    
    select ed.title,np.*, ed.*, dt.* from nepaccess.document_text dt
		inner join nepaccess.eisdoc ed on dt.document_id = ed.id
        inner join nepaccess.nepa_process np on ed.process_id = np.process_id
    where plaintext is not null
    and ed.process_id = 1000362;
   
    select count(1), ed.process_id, ed.title;
    
    select * from nepaccess_7_21.eisdoc;
    
    SELECT
		(select count(1) from nepaccess_7_21.eisdoc) as `Total Document Count`,
		(Select count(1) from nepaccess_7_21.eisdoc where comment_date is null) as `Missing Comment Date`,
        (Select count(1) from nepaccess_7_21.eisdoc where draft_noa is null) as `Missing Draft NOAs`,
        (Select count(1) from nepaccess_7_21.eisdoc where final_noa is null) as `Missing Final NOA`,
        (Select count(1) from nepaccess_7_21.eisdoc where noi_date is null) as `Missing NOI Date`,
        (select count(1) from nepaccess_7_21.eisdoc where draft_noa is null and final_noa is null) `mssing draft AND final NOA dates`,
        (select count(1) from nepaccess_7_21.eisdoc where draft_noa is null or final_noa is null) `mssing either the draft and or final dates`,
        (select count(1) from nepaccess_7_21.eisdoc where comment_date is not null and final_noa is not null) `Records with start date and end date)`,
        (select count(1) from nepaccess_7_21.eisdoc where comment_date is not null and draft_noa is not null and final_noa is not null) `Records eligible for time lines, start, draft and final `
;        
    select count(*) from nepaccess_7_21.eisdoc ed 
	where ed.id in 
	(select document_id from nepaccess_7_21.update_log ul where document_type in ('final','draft') and ul.document_id = ed.id);
    
    select count(title) from (
    select title,count(1) from nepaccess_7_21.update_log
    where document in ('final','draft')
    group by title
    )x;
    
    
    

		inner join nepaccess.nepa_process np on np.process_id = ed.process_id
    group by ed.process_id, ed.title
    order by count(1) desc;