package in.ajay.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import in.ajay.entity.EligEntity;

public interface EligRepo extends JpaRepository<EligEntity, Integer> {

	@Query("update EligEntity set planStatus=:status where edgTraceId=:edgTraceId")
	public Integer updatePlanStatus(Integer edgTraceId, String status);
	
	public EligEntity findByCaseNum(Integer caseNum);
}
