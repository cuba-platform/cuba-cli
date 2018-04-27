package ${listener.packageName};
#set( $interfaces = "" )
#set( $entity = ${listener.entityName} )

import ${listener.entityPackageName}.$entity;
import org.springframework.stereotype.Component;
#if( ${listener.beforeInsert} )
import com.haulmont.cuba.core.listener.BeforeInsertEntityListener;
#set( $interfaces = ${names.join($interfaces,"BeforeInsertEntityListener<$entity>",", ")} )
#end
#if( ${listener.beforeUpdate} )
import com.haulmont.cuba.core.listener.BeforeUpdateEntityListener;
#set( $interfaces = ${names.join($interfaces,"BeforeUpdateEntityListener<$entity>",", ")} )
#end
#if( ${listener.beforeDelete} )
import com.haulmont.cuba.core.listener.BeforeDeleteEntityListener;
#set( $interfaces = ${names.join($interfaces,"BeforeDeleteEntityListener<$entity>",", ")} )
#end
#if( ${listener.afterInsert} )
import com.haulmont.cuba.core.listener.AfterInsertEntityListener;
#set( $interfaces = ${names.join($interfaces,"AfterInsertEntityListener<$entity>",", ")} )
#end
#if( ${listener.afterUpdate} )
import com.haulmont.cuba.core.listener.AfterUpdateEntityListener;
#set( $interfaces = ${names.join($interfaces,"AfterUpdateEntityListener<$entity>",", ")} )
#end
#if( ${listener.afterDelete} )
import com.haulmont.cuba.core.listener.AfterDeleteEntityListener;
#set( $interfaces = ${names.join($interfaces,"AfterDeleteEntityListener<$entity>",", ")} )
#end
#if( ${listener.beforeAttach} )
import com.haulmont.cuba.core.listener.BeforeAttachEntityListener;
#set( $interfaces = ${names.join($interfaces,"BeforeAttachEntityListener<$entity>",", ")} )
#end
#if( ${listener.beforeDetach} )
import com.haulmont.cuba.core.listener.BeforeDetachEntityListener;
#set( $interfaces = ${names.join($interfaces,"BeforeDetachEntityListener<$entity>",", ")} )
#end
#if( ${listener.afterInsert} || ${listener.afterUpdate} || ${listener.afterDelete} )
import java.sql.Connection;
#end
#if( ${listener.beforeInsert} || ${listener.beforeUpdate} || ${listener.beforeDelete} || ${listener.beforeDetach} )
import com.haulmont.cuba.core.EntityManager;
#end

@Component("${listener.beanName}")
public class ${listener.className} implements $interfaces {

#if( ${listener.beforeInsert} )

    @Override
    public void onBeforeInsert(${listener.entityName} entity, EntityManager entityManager) {

    }
#end
#if( ${listener.beforeUpdate} )

    @Override
    public void onBeforeUpdate(${listener.entityName} entity, EntityManager entityManager) {

    }
#end
#if( ${listener.beforeDelete} )

    @Override
    public void onBeforeDelete(${listener.entityName} entity, EntityManager entityManager) {

    }
#end
#if( ${listener.afterInsert} )

    @Override
    public void onAfterInsert(${listener.entityName} entity, Connection connection) {

    }
#end
#if( ${listener.afterUpdate} )

    @Override
    public void onAfterUpdate(${listener.entityName} entity, Connection connection) {

    }
#end
#if( ${listener.afterDelete} )

    @Override
    public void onAfterDelete(${listener.entityName} entity, Connection connection) {

    }
#end
#if( ${listener.beforeAttach} )

    @Override
    public void onBeforeAttach(${listener.entityName} entity) {

    }
#end
#if( ${listener.beforeDetach} )

    @Override
    public void onBeforeDetach(${listener.entityName} entity, EntityManager entityManager) {

    }
#end

}