package com.starter.springboot.domain;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(BaseAuditedEntity.class)
public abstract class BaseAuditedEntity_ {

	public static volatile SingularAttribute<BaseAuditedEntity, LocalDateTime> createdDate;
	public static volatile SingularAttribute<BaseAuditedEntity, LocalDateTime> lastModifiedDate;
	public static volatile SingularAttribute<BaseAuditedEntity, String> createdBy;
	public static volatile SingularAttribute<BaseAuditedEntity, String> lastModifiedBy;

	public static final String CREATED_DATE = "createdDate";
	public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
	public static final String CREATED_BY = "createdBy";
	public static final String LAST_MODIFIED_BY = "lastModifiedBy";

}

