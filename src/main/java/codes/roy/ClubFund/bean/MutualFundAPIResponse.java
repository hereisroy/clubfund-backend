package codes.roy.ClubFund.bean;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MutualFundAPIResponse {

	ResponseMetaData meta;
	List<ResponseData> data;
	String status;

	public ResponseMetaData getMeta() {
		return meta;
	}
	public void setMeta(ResponseMetaData metaData) {
		this.meta = metaData;
	}
	public List<ResponseData> getData() {
		return data;
	}
	public void setData(List<ResponseData> data) {
		this.data = data;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	public static class ResponseMetaData{
		@JsonProperty("fund_house")
		private String fundHouse;
		@JsonProperty("scheme_type")
		private String schemeType;
		@JsonProperty("scheme_category")
		private String schemeCategory;
		@JsonProperty("scheme_code")
		private int schemeCode;
		@JsonProperty("scheme_name")
		private String schemeName;
		public String getFundHouse() {
			return fundHouse;
		}
		public void setFundHouse(String fundHouse) {
			this.fundHouse = fundHouse;
		}
		public String getSchemeType() {
			return schemeType;
		}
		public void setSchemeType(String schemeType) {
			this.schemeType = schemeType;
		}
		public String getSchemeCategory() {
			return schemeCategory;
		}
		public void setSchemeCategory(String schemeCategory) {
			this.schemeCategory = schemeCategory;
		}
		public int getSchemeCode() {
			return schemeCode;
		}
		public void setSchemeCode(int schemeCode) {
			this.schemeCode = schemeCode;
		}
		public String getSchemeName() {
			return schemeName;
		}
		public void setSchemeName(String scheme_name) {
			this.schemeName = scheme_name;
		}

	}

	public static class ResponseData{
		@JsonFormat(pattern = "dd-MM-yyyy")
		LocalDate date;
		String nav;

		public LocalDate getDate() {
			return date;
		}
		public void setDate(LocalDate date) {
			this.date = date;
		}
		public String getNav() {
			return nav;
		}
		public void setNav(String nav) {
			this.nav = nav;
		}
		@Override
		public String toString() {
			return "Date : " + date + " | NAV : " + nav;
		}
	}

}
