package in.ajay.service;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;

import in.ajay.entity.CoNoticeEntity;
import in.ajay.entity.EligEntity;
import in.ajay.repo.CoNoticeRepo;
import in.ajay.repo.EligRepo;
import in.ajay.utils.EmailUtils;

@Service
public class CoServiceImpl implements CoService{

	//@Autowired
	//private CoTriggerRepository coTrgRepo; 
	
	//@Autowired
	//private CitizenAppRepository appRepo; 
	
	//@Autowired
	//private DcCaseRepo dcCaseRepo; 
	
	@Autowired
	private CoNoticeRepo noticeRepo;
	
	@Autowired
	private EligRepo eligRepo;
	
	@Autowired
	private EmailUtils emailUtils;
	
	@Autowired
	private AmazonS3 s3; 
	
	@Value("${bucketName}")
	private String bucketName;
	
	
	@Override
	public void processPendingTriggers() {
		
		// fetch all pending triggers (records) from CO_NOTICES table
		List<CoNoticeEntity> records = noticeRepo.findByNoticeStatus("P");
		
		// To improve the performance batch job we have multi-threding logic as below: 
		
		ExecutorService exService = Executors.newFixedThreadPool(10);
		for(CoNoticeEntity trigger : records) {
			exService.submit(new Runnable() {
				@Override
				public void run() {
					//  Execute the tasks here
					processEachRecord(trigger);
					//processTrigger(entity);
				}
			});
		}
	}
	// End of Multi-threading logic
	
	/*
	public CoResponse processPendingTriggers() throws Exception {
		CoResponse response = new CoResponse();
		//fetch all pending triggers
		List<CoTriggerEntity> pendingTrgs = coTrgRepo.findByTrgStatus("P");
		for (CoTriggerEntity trigger : pendingTrgs) {
			processTrigger(trigger);
		}
		return null;
	}
*/
	
/*
 	private CitizenAppEntity processTrigger(CoTriggerEntity entity) throws Exception {
 	
 		CitizenAppEntity appEntity = null;
 		//get eligibility data based on case number
 		
 		EligDtlsEntity elig = eligRepo.findByCaseNum(entity.getCaseNum());
 		
 		//get citizen data based on case number
 		 
 		Optional<DcCaseEntity> findById = dcCaseRepo.findById(entity.getCaseNum());
 		if(findById.isPresent()){
 			DcCaseEntity dcCaseEntity = findById.get();
 			Integer appId = dcCaseEntity.getAppId();
 			Optional<CitizenAppEntity> appEntityOptional = appRepo.findById(appId);
 			if(appEntityOptional.isPresent()){
 				appEntity = appEntityOptional.get();
 			}
 		} 
 		
 		String planStatus = elig.getPlanStatus();
 		File file = null;
		if("AP".equals(planStatus)) {
			file = generateApprovedNotice(elig, appEntity);
		}else if("DN".equals(planStatus)){
			file = generateDeniedNotice(elig, appEntity);
		}
		
Case 1:		//String objUrl = uploadToS3(file);
		      updateTrigger(elig.getCaseNum(), null);
		
Case 2:		//String objUrl = uploadToS3(file);
		    //updateTrigger(elig.getCaseNum(), objUrl);
	 	
	 	return appEntity;
 	}
 	
 	private String uploadToS3(File file){
 	
 		PutObjectResult putObjectResult = s3.putObject(bucketName, file.getName(), file);
 		URL url = s3.getUrl(bucketName, file.getName());
 		return url.toExternalForm();
 	}
 	
 	private File generateAndSendApPdf(EligDtlsEntity eligData, CitizenAppEntity appEntity)
 	throws Exception {
 	
 		Document document = new Document(PageSize.A4);
 		
 		 File file = new File(eligData.getCaseNum() + ".pdf");
 		 FileOutputStream fos = null;
 		 try{
 		 		fos = new FileOutputStream(file);
 		 }catch(FileNotFoundException e){
 		 		e.printStackTrace();
 		 }
 		 PdfWriter.getInstance(document, fos);
 		 document.open();
 		 
 		 Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
 		 font.SetSize(18);
 		 font.setColor(Color.BLUE);
 		 
 		 Paragraph p = new Paragraph("ELIGIBILTY REPORT", font);
 		 p.setAlignment(Paragraph.ALIGN_CENTER);
 		 
 		 document.add(p);
 		 
 		 PdfPTable table = new PdfPTable(6);
 		 table.setWidthPercentage(100f);
 		 table.setWidths(new float[] { 1.5f, 3.5f, 3.0f, 1.5f, 3.0f, 1.5f });
 		 table.setSpacingBefore(10);
 		 
 		 PdfPCell cell = new PdfPCell();
 		 cell.setBackgroundColor(Color.BLUE);
 		 cell.setPadding(5);
 		 
 		 font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
 		 font.setColor(Color.WHITE);
 		 
 		 cell.setPhrase(new Phrase("Citizen Name", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Plan Name", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Plan Status", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Plan Start Date", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Plan End Date", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Benefit Amount", font));
 		 table.addCell(cell);
 		 
 		 table.addCell(appEntity.getFullName());
 		 table.addCell(eligData.getPlanName());
 		 table.addCell(eligData.getPlanStatus());
 		 table.addCell(eligData.getPlanStartDate() + "");
 		 table.addCell(eligData.getPlanEndDate() + "");
 		 table.addCell(eligData.getBenefitAmt() + "");
 		 
 		 document.add(table);
 		 document.close();
 		 
 		 String subject = "IES ELIGIBILTY INFO";
 		 String body = "IES ELIGIBILTY INFO";
 		 
 		 emailUtils.sendEmail(appEntity.getEmail(), subject, body, file);
 		 
 		 return file;
 	}
 	
 	private File generateAndSendDnPdf(EligDtlsEntity eligData, CitizenAppEntity appEntity)
 	throws Exception {
 	
 		Document document = new Document(PageSize.A4);
 		
 		 File file = new File(eligData.getCaseNum() + ".pdf");
 		 FileOutputStream fos = null;
 		 try{
 		 		fos = new FileOutputStream(file);
 		 }catch(FileNotFoundException e){
 		 		e.printStackTrace();
 		 }
 		 PdfWriter.getInstance(document, fos);
 		 document.open();
 		 
 		 Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
 		 font.SetSize(18);
 		 font.setColor(Color.BLUE);
 		 
 		 Paragraph p = new Paragraph("ELIGIBILTY REPORT", font);
 		 p.setAlignment(Paragraph.ALIGN_CENTER);
 		 
 		 document.add(p);
 		 
 		 PdfPTable table = new PdfPTable(4);
 		 table.setWidthPercentage(100f);
 		 table.setWidths(new float[] { 1.5f, 3.5f, 3.0f, 1.5f });
 		 table.setSpacingBefore(10);
 		 
 		 PdfPCell cell = new PdfPCell();
 		 cell.setBackgroundColor(Color.BLUE);
 		 cell.setPadding(5);
 		 
 		 font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
 		 font.setColor(Color.WHITE);
 		 
 		 cell.setPhrase(new Phrase("Citizen Name", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Plan Name", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Plan Status", font));
 		 table.addCell(cell);
 		 
 		 cell.setPhrase(new Phrase("Denial Reason", font));
 		 table.addCell(cell);
 		 
 		 table.addCell(appEntity.getFullName());
 		 table.addCell(eligData.getPlanName());
 		 table.addCell(eligData.getPlanStatus());
  		 table.addCell(eligData.getDenialRsn() + "");
 		 
 		 document.add(table);
 		 document.close();
 		 
 		 String subject = "IES ELIGIBILTY INFO";
 		 String body = "IES ELIGIBILTY INFO";
 		 
 		 emailUtils.sendEmail(appEntity.getEmail(), subject, body, file);
 		 
 		 return file;
 	}
 	
 	private void updateTrigger(Long caseNum, String objUrl) throws Exception {
 		CoTriggerEntity coEntity = coTrgRepo.findByCaseNum(caseNum);
 		coEntity.setNoticeUrl(objUrl);
 		coEntity.setTrgStatus("C");
 		coTrgRepo.save(coEntity);
 	}
 	
 	
 	
 * */	
	private void processEachRecord(CoNoticeEntity entity) {
		Integer caseNum = entity.getCaseNum();
		
		// get eligibility data
		EligEntity eligEntity = eligRepo.findByCaseNum(caseNum);
		String planStatus = eligEntity.getPlanStatus();
		
		File pdfFile = null;
		if("AP".equals(planStatus)) {
			pdfFile = generateApprovedNotice(eligEntity);
		}else if("DN".equals(planStatus)){
			pdfFile = generateDeniedNotice(eligEntity);
		}
		String fileUrl = uploadToS3(pdfFile);
		boolean isUpdated = updateProcessedRecord(entity, fileUrl);
		
		if(isUpdated) {
			emailUtils.sendEmail("", "", "", pdfFile);
		}
	}

	private boolean updateProcessedRecord(CoNoticeEntity entity, String fileUrl) {
		
		// set status as Completed
		// set Notice S3 Object URL
		// update record in db

		entity.setNoticeStatus("H");
		entity.setNoticeUrl(fileUrl);
		
		noticeRepo.save(entity);
		return true;
		
	}

	private String uploadToS3(File pdfFile) {
		
		// logic to store in S3 
		return null;
	}
	
	private File generateDeniedNotice(EligEntity eligEntity) {
		// generate pdf file
		return null;
	}

	private File generateApprovedNotice(EligEntity eligEntity) {
		// generate pdf file
		return null;
	}
	
/* 	private void updateTrigger(Integer caseNum, String objUrl) throws Exception {
		CoTriggerEntity coEntity = coTrgRepo.findByCaseNum(caseNum);
		
		coEntity.setNoticeUrl(objUrl);
		coEntity.setTrgStatus("C");
		coTrgRepo.save(coEntity);
	}
*/	
}
