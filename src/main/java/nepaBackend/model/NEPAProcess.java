package nepaBackend.model;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Table(name="nepa_process") 
@Indexed
public class NEPAProcess {
	
	// internal ID with no target purpose except to make some operations easier honestly.
    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
//    @GenericField(name="process_id",projectable=Projectable.YES)
    private Long id; // ensure bigint(20) auto-increment, primary key

    // This should get set by an incoming spreadsheet (csv/tsv/...)
    @NotNull
    @Column(name="process_id", unique=true)
    private Long processId;
    
	// Optional foreign keys //
    
    @OneToOne
    @Nullable
    @JoinColumn(name="noi_id")
	private EISDoc docNoi;

    
    
    @OneToOne
    @Nullable
    @JoinColumn(name="draft_id")
	private EISDoc docDraft;
    
    
    @OneToOne
    @Nullable
    @JoinColumn(name="revdraft_id")
	private EISDoc docRevisedDraft;
    
    @OneToOne
    @Nullable
    @JoinColumn(name="draftsup_id")
	private EISDoc docDraftSupplement;

	@OneToOne
    @Nullable
    @JoinColumn(name="secdraft_id")
	private EISDoc docSecondDraft;
    
    @OneToOne
    @Nullable
    @JoinColumn(name="secdraftsup_id")
	private EISDoc docSecondDraftSupplement;
    @OneToOne
    @Nullable
    @JoinColumn(name="thirddraftsup_id")
	private EISDoc docThirdDraftSupplement;

    
    
	@OneToOne
    @Nullable
    @JoinColumn(name="final_id")
	private EISDoc docFinal;

    @OneToOne
    @Nullable
    @JoinColumn(name="revfinal_id")
	private EISDoc docRevisedFinal;

    @OneToOne
    @Nullable
    @JoinColumn(name="finalsup_id")
	private EISDoc docFinalSupplement;
    
    @OneToOne
    @Nullable
    @JoinColumn(name="secfinal_id")
	private EISDoc docSecondFinal;
    
    @OneToOne
    @Nullable
    @JoinColumn(name="secfinalsup_id")
	private EISDoc docSecondFinalSupplement;

    @OneToOne
    @Nullable
    @JoinColumn(name="thirdfinalsup_id")
	private EISDoc docThirdFinalSupplement;
    
    
    
    @OneToOne
    @Nullable
    @JoinColumn(name="rod_id")
	private EISDoc docRod;
    
    
    @OneToOne
    @Nullable
    @JoinColumn(name="scoping_id")
	private EISDoc docScoping;

    
    @OneToOne
    @Nullable
    @JoinColumn(name="epacomments_id")
	private EISDoc docEpaComments;

    
	public NEPAProcess(Long processId) {
		this.setProcessId(processId);
	}


	public Long getId() {
		return id;
	}
	

	public Long getProcessId() {
		return processId;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}


	public EISDoc getDocNoi() {
		return docNoi;
	}

	public void setDocNoi(EISDoc docNoi) {
		this.docNoi = docNoi;
	}

	public EISDoc getDocDraft() {
		return docDraft;
	}

	public void setDocDraft(EISDoc docDraft) {
		this.docDraft = docDraft;
	}

	public EISDoc getDocRevisedDraft() {
		return docRevisedDraft;
	}

	public void setDocRevisedDraft(EISDoc docRevisedDraft) {
		this.docRevisedDraft = docRevisedDraft;
	}
    
    public EISDoc getDocSecondDraft() {
		return docSecondDraft;
	}

	public void setDocSecondDraft(EISDoc docSecondDraft) {
		this.docSecondDraft = docSecondDraft;
	}

	public EISDoc getDocDraftSupplement() {
		return docDraftSupplement;
	}

	public void setDocDraftSupplement(EISDoc docDraftSupplement) {
		this.docDraftSupplement = docDraftSupplement;
	}

	public EISDoc getDocSecondDraftSupplement() {
		return docSecondDraftSupplement;
	}

	public void setDocSecondDraftSupplement(EISDoc docSecondDraftSupplement) {
		this.docSecondDraftSupplement = docSecondDraftSupplement;
	}

	public EISDoc getDocThirdDraftSupplement() {
		return docThirdDraftSupplement;
	}

	public void setDocThirdDraftSupplement(EISDoc docThirdDraftSupplement) {
		this.docThirdDraftSupplement = docThirdDraftSupplement;
	}

	public EISDoc getDocFinal() {
		return docFinal;
	}

	public void setDocFinal(EISDoc docFinal) {
		this.docFinal = docFinal;
	}

	public EISDoc getDocRevisedFinal() {
		return docRevisedFinal;
	}

	public void setDocRevisedFinal(EISDoc docRevisedFinal) {
		this.docRevisedFinal = docRevisedFinal;
	}

	public EISDoc getDocFinalSupplement() {
		return docFinalSupplement;
	}

	public void setDocFinalSupplement(EISDoc docFinalSupplement) {
		this.docFinalSupplement = docFinalSupplement;
	}

	public EISDoc getDocSecondFinal() {
		return docSecondFinal;
	}

	public void setDocSecondFinal(EISDoc docSecondFinal) {
		this.docSecondFinal = docSecondFinal;
	}

	public EISDoc getDocSecondFinalSupplement() {
		return docSecondFinalSupplement;
	}

	public void setDocSecondFinalSupplement(EISDoc docSecondFinalSupplement) {
		this.docSecondFinalSupplement = docSecondFinalSupplement;
	}

	public EISDoc getDocThirdFinalSupplement() {
		return docThirdFinalSupplement;
	}

	public void setDocThirdFinalSupplement(EISDoc docThirdFinalSupplement) {
		this.docThirdFinalSupplement = docThirdFinalSupplement;
	}

	public EISDoc getDocRod() {
		return docRod;
	}

	public void setDocRod(EISDoc docRod) {
		this.docRod = docRod;
	}

	public EISDoc getDocScoping() {
		return docScoping;
	}

	public void setDocScoping(EISDoc docScoping) {
		this.docScoping = docScoping;
	}

	public EISDoc getDocEpaComments() {
		return docEpaComments;
	}

	public void setDocEpaComments(EISDoc docEpaComments) {
		this.docEpaComments = docEpaComments;
	}

}
