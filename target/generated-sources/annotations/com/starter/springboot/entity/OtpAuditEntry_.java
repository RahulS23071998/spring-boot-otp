package com.starter.springboot.entity;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(OtpAuditEntry.class)
public abstract class OtpAuditEntry_ extends com.starter.springboot.entity.BaseAuditedEntity_ {

	public static volatile SingularAttribute<OtpAuditEntry, LocalDate> expiresOn;
	public static volatile SingularAttribute<OtpAuditEntry, Long> id;
	public static volatile SingularAttribute<OtpAuditEntry, LocalDate> issuedOn;
	public static volatile SingularAttribute<OtpAuditEntry, String> partnerExpiry;
	public static volatile SingularAttribute<OtpAuditEntry, String> username;

	public static final String EXPIRES_ON = "expiresOn";
	public static final String ID = "id";
	public static final String ISSUED_ON = "issuedOn";
	public static final String PARTNER_EXPIRY = "partnerExpiry";
	public static final String USERNAME = "username";

}

