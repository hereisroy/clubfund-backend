package codes.roy.ClubFund.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import codes.roy.ClubFund.model.Contribution;
import codes.roy.ClubFund.model.Investment;
import codes.roy.ClubFund.model.Member;
import codes.roy.ClubFund.model.MutualFund;
import codes.roy.ClubFund.model.MutualFundNav;
import codes.roy.ClubFund.util.ClubFundUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

@Repository
@Transactional
public class AllDataRepository {

	@Autowired
	private EntityManager em;

	// Mutual Fund Methods

	public boolean isValidFundId(int fundId) {
		Query query = em.createQuery("SELECT COUNT(mf) FROM MutualFund mf WHERE mf.fundId=:fundId");
		query.setParameter("fundId", fundId);
		return ((long)query.getSingleResult()==0)? false : true;
	}

	public List<MutualFund> getAllMutualFunds(){
		TypedQuery<MutualFund> tq = em.createQuery("SELECT mf FROM MutualFund mf", MutualFund.class);
		return tq.getResultList();
	}

	public List<MutualFund> getAllMutualFundsInOrder(){
		TypedQuery<MutualFund> tq = em.createQuery("SELECT mf FROM MutualFund mf ORDER BY mf.netInvestment DESC", MutualFund.class);
		return tq.getResultList();
	}

	@SuppressWarnings("unchecked")
	private List<MutualFund> getFundsHavingInvestments(LocalDate date){
		Query query = em.createNativeQuery("SELECT * FROM mutual_fund WHERE fund_id IN (SELECT DISTINCT fund_fund_id FROM investment WHERE investment_date<=:date)", MutualFund.class);
		query.setParameter("date", date);
		return query.getResultList();
	}

	public List<MutualFund> getFundsRequiringNav(LocalDate date){
		LocalDate latestNavDate = getLatestNavDate();
		if(latestNavDate!=null && latestNavDate.isEqual(date)) {
			return getAllMutualFunds();
		} else {
			return getFundsHavingInvestments(date);
		}
	}

	@SuppressWarnings("unchecked")
	public List<MutualFund> getFundsNotHavingInvestments(){
		Query query = em.createNativeQuery("SELECT * FROM mutual_fund WHERE fund_id NOT IN (SELECT DISTINCT fund_fund_id FROM investment)", MutualFund.class);
		return query.getResultList();
	}

	public MutualFund getMutualFundById(int fundId) {
		return em.find(MutualFund.class, fundId);
	}

	public double getAvgXIRR() {
		Query query = em.createNativeQuery("SELECT AVG(xirr) FROM mutual_fund WHERE net_investment>0");
		return (double)Optional.ofNullable(query.getSingleResult()).orElse(0D);
	}

	public void addMutualFund(MutualFund newMutualFund) {
		em.persist(newMutualFund);
	}

	public void updateMutualFund(MutualFund existingMutualFund) {
		em.merge(existingMutualFund);
	}

	public void deleteMutualFund(MutualFund existingMutualFund) {
		em.remove(existingMutualFund);
		em.flush();
	}

	// MutualFundNav Methods

	public boolean hasNav(MutualFund fund,LocalDate date) {
		Query query = em.createQuery("SELECT COUNT(nav) FROM MutualFundNav nav WHERE nav.fund=:fund and nav.date=:date");
		query.setParameter("fund", fund);
		query.setParameter("date", date);
		return ((long)query.getSingleResult()>0)? true : false;
	}

	public MutualFundNav getNav(MutualFund fund,LocalDate date) {
		TypedQuery<MutualFundNav> query = em.createQuery("SELECT nav FROM MutualFundNav nav WHERE nav.fund=:fund and nav.date=:date", MutualFundNav.class);
		query.setParameter("fund", fund);
		query.setParameter("date", date);
		MutualFundNav res = null;
		try{
			res = query.getSingleResult();
		} catch(NoResultException ex) {}

		return res;
	}

	public List<MutualFundNav> getAllNav(LocalDate date){
		TypedQuery<MutualFundNav> query = em.createQuery("SELECT nav from MutualFundNav nav WHERE nav.date IN :date", MutualFundNav.class);
		query.setParameter("date", date);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<MutualFundNav> getAllLatestNav() {
		Query query = em.createNativeQuery("SELECT * FROM `mutual_fund_nav` WHERE date=(SELECT date FROM `mutual_fund_nav` ORDER BY date DESC LIMIT 1)", MutualFundNav.class);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Integer> getFundIdsHavingLatestNav(){
		List<Integer> res = new ArrayList<>();
		LocalDate latestNavDate = getLatestNavDate();
		if(latestNavDate!=null) {
			Query query = em.createNativeQuery("SELECT DISTINCT fund_fund_id FROM mutual_fund_nav WHERE date=:date");
			query.setParameter("date", latestNavDate);
			res = query.getResultList();
		}
		return res;
	}

	public LocalDate getLatestNavDate() {
		Query query = em.createNativeQuery("SELECT date FROM `mutual_fund_nav` ORDER BY date DESC LIMIT 1", LocalDate.class);
		LocalDate result = null;
		try { result = (LocalDate)query.getSingleResult(); }
		catch(NoResultException nrex) {}
		return result;
	}

	public List<MutualFundNav> getAllInvNav(){
		TypedQuery<MutualFundNav> tq = em.createQuery("SELECT mfn FROM MutualFundNav mfn WHERE isInvestmentDate=:isInvDate", MutualFundNav.class);
		tq.setParameter("isInvDate", true);
		return tq.getResultList();
	}

	public void addNav(MutualFundNav nav) {
		em.persist(nav);
	}

	public MutualFundNav updateNav(MutualFundNav nav) {
		return em.merge(nav);
	}

	public void detachNavsFromInvestments(LocalDate invDate) {
		Query query = em.createQuery("SELECT count(inv) FROM Investment inv WHERE inv.investmentDate=:navDate");
		query.setParameter("navDate", invDate);
		long invCount = 0;
		try { invCount = (long)query.getSingleResult(); }
		catch(NoResultException ex) {}
		if(invCount==0) {
			Query updateQuery = em.createQuery("UPDATE MutualFundNav nav SET nav.isInvestmentDate=false WHERE nav.date=:date");
			updateQuery.setParameter("date", invDate);
			updateQuery.executeUpdate();
		}
	}

	public void deleteAllNavsOfFund(int fundId) {
		Query query = em.createNativeQuery("DELETE FROM mutual_fund_nav WHERE fund_fund_id=:fundId");
		query.setParameter("fundId", fundId);
		query.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	public void deleteUnnecessaryNavs() {
		Query subQ = em.createNativeQuery("SELECT DISTINCT date FROM mutual_fund_nav ORDER BY date DESC LIMIT 2");
		List<LocalDate> latestDates = subQ.getResultList();
		Query query = em.createNativeQuery("DELETE FROM mutual_fund_nav WHERE is_investment_date=0 AND date NOT IN(:latestDates)");
		query.setParameter("latestDates", latestDates);
		query.executeUpdate();
		System.out.println("Deleted Unnecessary Data");
	}

	// Member Methods

	public boolean isValidMemberId(int memberId) {
		Query query = em.createQuery("SELECT COUNT(m) FROM Member m WHERE m.memberId = :memberId");
		query.setParameter("memberId", memberId);
		return ((long)query.getSingleResult()==0)? false : true;
	}

	public Member getMemberById(int memberId) {
		return em.find(Member.class, memberId);
	}

	public Member getMemberByEmail(String email) {
		TypedQuery<Member> tq = em.createQuery("SELECT m FROM Member m WHERE m.email= :email", Member.class);
		tq.setParameter("email", email);
		Member res = null;
		List<Member> memberList = tq.getResultList();
		if(memberList.size()>0) {
			res = memberList.get(0);
		}
		return res;
	}

	public List<Member> getAllMembers(){
		TypedQuery<Member> tq = em.createQuery("SELECT m from Member m", Member.class);
		return tq.getResultList();
	}

	public int getAdminCount() {
		Query query = em.createQuery("SELECT COUNT(mem) FROM Member mem WHERE mem.isAdmin=TRUE AND mem.email<>:idealEmail");
		query.setParameter("idealEmail", ClubFundUtil.idealMemberEmailId);
		return Integer.parseInt(query.getResultList().get(0).toString());
	}

	public void addMember(Member newMember) {
		em.persist(newMember);
	}

	public void updateMember(Member existingMember) {
		em.merge(existingMember);
	}

	public void deleteMember(Member existingMember) {
		em.remove(existingMember);
	}
	
	public double getMaxNC() {
		Query query = em.createQuery("SELECT MAX(mem.nc) FROM Member mem");
		double max = 0D;
		try { max = Double.parseDouble(query.getSingleResult().toString()); }
		catch(Exception ex) {  }
		return max;
	}

	// Contribution Methods

	public Contribution getContributionById(int contributionId) {
		return em.find(Contribution.class, contributionId);
	}

	public int getCurrentIdealContributionAmmount() {
		Query query = em.createNativeQuery("SELECT ammount FROM `contribution` WHERE contributor_member_id = (SELECT member_id FROM member WHERE email=:email) AND investment_investment_id = (SELECT investment_id FROM investment ORDER BY investment_date DESC LIMIT 1)");
		query.setParameter("email", ClubFundUtil.idealMemberEmailId);
		return (int)query.getSingleResult();
	}

	public void addContribution(Contribution newContribution) {
		em.persist(newContribution);
	}

	public Contribution updateContribution(Contribution existingContribution) {
		return em.merge(existingContribution);
	}

	public void deleteContribution(Contribution existingContribution) {
		em.remove(existingContribution);
	}

	// Investment Methods

	public List<Investment> getAllInvestments(){
		TypedQuery<Investment> tq = em.createQuery("SELECT inv FROM Investment inv ORDER BY inv.investmentDate DESC", Investment.class);
		return tq.getResultList();
	}

	public List<Investment> getAllInvestmentsInASC(){
		TypedQuery<Investment> tq = em.createQuery("SELECT inv FROM Investment inv ORDER BY inv.investmentDate", Investment.class);
		return tq.getResultList();
	}

	public List<Investment> getInvestmentsOn(LocalDate date){
		TypedQuery<Investment> tq = em.createQuery("SELECT inv FROM Investment inv WHERE inv.investmentDate=:date", Investment.class);
		tq.setParameter("date", date);
		return tq.getResultList();
	}

	public List<Investment> getInvestmentsBeforeDate(LocalDate date, int count){
		TypedQuery<Investment> tq = em.createQuery("SELECT inv FROM Investment inv WHERE inv.investmentDate<:date ORDER BY inv.investmentDate DESC", Investment.class);
		tq.setParameter("date", date);
		tq.setMaxResults(count);
		return tq.getResultList();
	}

	public List<Investment> getInvestmentsByYear(int year){
		LocalDate firstDate = LocalDate.of(year, 1, 1);
		LocalDate lastDate = LocalDate.of(year, 12, 31);
		TypedQuery<Investment> tq = em.createQuery("SELECT inv FROM Investment inv WHERE inv.investmentDate BETWEEN :firstDate AND :lastDate ORDER BY inv.investmentDate DESC", Investment.class);
		tq.setParameter("firstDate", firstDate);
		tq.setParameter("lastDate", lastDate);
		return tq.getResultList();
	}

	public Investment getInvestmentById(int investmentId) {
		return em.find(Investment.class, investmentId);
	}

	@SuppressWarnings("unchecked")
	public List<Integer> getInvestmentYears(){
		Query query = em.createNativeQuery("SELECT DISTINCT YEAR(investment_date) AS year FROM investment ORDER BY year DESC");
		return query.getResultList();
	}

	public int getInvCount(MutualFund fund) {
		Query query = em.createQuery("SELECT COUNT(inv) FROM Investment inv WHERE inv.fund=:fund");
		query.setParameter("fund", fund);
		long count = 0;
		try {count=(long)query.getSingleResult();}
		catch(NoResultException noResEx) {}
		return (int)count;
	}
	
	public int getInvNo(Investment inv) {
		Query query = em.createQuery("SELECT COUNT(inv) FROM Investment inv WHERE inv.investmentDate<=:date AND inv.investmentId<:id");
		query.setParameter("date", inv.getInvestmentDate());
		query.setParameter("id", inv.getInvestmentId());
		long invNo = 0;
		try {invNo=(long)query.getSingleResult() + 1;}
		catch(NoResultException noResEx) {}
		return (int)invNo;
	}

	public void addInvestment(Investment newInvestment) {
		em.persist(newInvestment);
	}

	public void updateInvestment(Investment existingInvestment) {
		em.merge(existingInvestment);
	}

	public void deleteInvestment(Investment existingInvestment) {
		em.remove(existingInvestment);
	}


}
