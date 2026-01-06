package com.starter.springboot.entity;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.Instant;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RefreshToken.class)
public abstract class RefreshToken_ {

	public static volatile SingularAttribute<RefreshToken, Instant> createdAt;
	public static volatile SingularAttribute<RefreshToken, String> replacedByToken;
	public static volatile SingularAttribute<RefreshToken, String> ipAddress;
	public static volatile SingularAttribute<RefreshToken, String> userAgent;
	public static volatile SingularAttribute<RefreshToken, Long> id;
	public static volatile SingularAttribute<RefreshToken, Boolean> isActive;
	public static volatile SingularAttribute<RefreshToken, Long> userId;
	public static volatile SingularAttribute<RefreshToken, Instant> revokedAt;
	public static volatile SingularAttribute<RefreshToken, Instant> expiresAt;
	public static volatile SingularAttribute<RefreshToken, String> token;

	public static final String CREATED_AT = "createdAt";
	public static final String REPLACED_BY_TOKEN = "replacedByToken";
	public static final String IP_ADDRESS = "ipAddress";
	public static final String USER_AGENT = "userAgent";
	public static final String ID = "id";
	public static final String IS_ACTIVE = "isActive";
	public static final String USER_ID = "userId";
	public static final String REVOKED_AT = "revokedAt";
	public static final String EXPIRES_AT = "expiresAt";
	public static final String TOKEN = "token";

}

