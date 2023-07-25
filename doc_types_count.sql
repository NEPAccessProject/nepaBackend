use nepaccess_7_21;
SELECT 
	(select count(*) from nepaccess_7_21.nepa_process) as `Count of all Nepa Processes`,
	(select count(*)  from nepaccess_7_21.eisdoc where process_id in (select process_id from nepa_process)) as `Count of EISDocs with process_ids`,
    	(select count(*) from nepaccess_7_21.eisdoc) as `Count of all EISDocs`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where filename is not null) as `Count of  all EISDoc Records with downloads`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where filename is null) as `Count of all EISDoc Records without downloads`,
	(select count(*)  from nepaccess_7_21.eisdoc ed  where ed.process_id is null) as `Count of EISDocs without process_ids`,

	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "Final") as `Count of all EISdocs with the document type of "Final"`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "final" and filename is null) as `Count of all EISdocs with the document type of "Final" without downloads`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "final" and filename is not null) as `Count of all EISdocs with the document type of "Final with downloads"`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type="draft") as `Count of all EISdocs with the document type of "Draft"`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "Draft" and filename is not null) as `Count of all EISdocs with the document type of "Draft with downloads`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "Draft" and filename is null) as `Count of all EISdocs with the document type of "Draft without Downloads`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "Final") as `Count of all EISDocs with the document type of "final"`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "Final" and filename is null) as `Count of all EISDocs with the document type of "final" without Downloads`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "Final" and filename is not null) as `Count of all EISDocs with the document type of "final" with Downloads`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "updated ROD") as `Count of all EISDocs with the document type of "Updated ROD"`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "updated ROD" and filename is not null) as `Count of all EISDocs with the document type of "Updated ROD"`,
	(SELECT Count(*) from nepaccess_7_21.eisdoc where document_type = "updated ROD" and filename is null) as `Count of all EISDocs with the document type of "Updated ROD" without Downloads`,
	(Select count(*) from nepaccess_7_21.eisdoc where document_type like '%Supplemental%') as `Count of all EISDocs with the document type(s) with "Supplemental" in the document_type`,
	(Select count(*) from nepaccess_7_21.eisdoc where document_type like '%Supplemental%' and filename is null) as `Count of all EISDocs with the document type(s) with "Supplemental" in the document type without Downloads`,
    (Select count(*) from nepaccess_7_21.eisdoc where document_type like '%Supplemental%' and filename is not null) as ` of all EISDocs with the document type(s) with "Supplemental" in the document type with Downloads`;
    


