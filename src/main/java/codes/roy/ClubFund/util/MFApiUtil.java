package codes.roy.ClubFund.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import codes.roy.ClubFund.bean.MutualFundAPIResponse;
import codes.roy.ClubFund.model.MutualFund;
import codes.roy.ClubFund.model.MutualFundNav;
import codes.roy.ClubFund.repository.AllDataRepository;

@Component
@Transactional
public class MFApiUtil implements MutualFundAPIUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(MFApiUtil.class);

	@Autowired
	AllDataRepository repo;

	@Override
	public boolean fetchAllLatestNavs() {
		boolean status = false;
		List<MutualFund> allFunds =  repo.getAllMutualFunds();
		if(allFunds.size()>0) {
			status = true;
		}
		String url="";
		MutualFundAPIResponse response;
		RestTemplate template = new RestTemplate();
		HashMap<Integer, Double> latestNavMap = new HashMap<>();
		LocalDate latestNavDate = null;
		try {
			for(MutualFund fund : allFunds) {
				url = fund.getApiURL() + "/latest";
				response = template.getForObject(url, MutualFundAPIResponse.class);
				latestNavMap.put(fund.getFundId(), Double.parseDouble(response.getData().get(0).getNav()));
				if(latestNavDate==null) {
					latestNavDate = response.getData().get(0).getDate();
					if(repo.getLatestNavDate()!=null && latestNavDate.isEqual(repo.getLatestNavDate())) {
						return false;
					}
				}
				else if(!latestNavDate.isEqual(response.getData().get(0).getDate())) {
					return false;
				}
			}
			for(MutualFund fund : allFunds) {
				MutualFundNav newFundNav = new MutualFundNav(fund,latestNavDate,latestNavMap.get(fund.getFundId()), false);
				repo.addNav(newFundNav);
			}
		} catch(Exception e) {
			status = false;
			LOGGER.error("MFApiUtil | updateAllLatestNav | API URL : " + url);
			LOGGER.error("MFApiUtil | updateAllLatestNav | Exception : ",e);
		}

		return status;
	}

	@Override
	public boolean fetchAndSaveRequiredFundNavs(LocalDate date, boolean isInvestmentDate) {
		boolean status = true;
		List<MutualFund> funds =  repo.getFundsRequiringNav(date);
		String url="";
		double nav;
		MutualFundAPIResponse response;
		RestTemplate template = new RestTemplate();
		try {
			for(MutualFund fund : funds) {
				nav = -1D;
				url = fund.getApiURL();
				response = template.getForObject(url, MutualFundAPIResponse.class);
				for(MutualFundAPIResponse.ResponseData navData : response.getData()) {
					if(date.isEqual(navData.getDate())) {
						nav = Double.parseDouble(navData.getNav());
						break;
					}
				}

				if(nav!=-1D) {
					MutualFundNav existingNav = repo.getNav(fund, date);
					if(existingNav!=null) {
						existingNav.setInvestmentDate(isInvestmentDate);
						existingNav.setNav(nav);
						repo.updateNav(existingNav);
					} else if(existingNav==null) {
						MutualFundNav newFundNav = new MutualFundNav(fund,date,nav,isInvestmentDate);
						repo.addNav(newFundNav);
					}
				} else {
					throw new Exception("No nav value found for date = " + date);
				}
			}
		} catch(Exception e) {
			status = false;
			LOGGER.error("MFApiUtil | fetchAndSaveAllFundNav | API URL : " + url);
			LOGGER.error("MFApiUtil | fetchAndSaveAllFundNav | Exception : ",e);
		}

		return status;
	}

}
