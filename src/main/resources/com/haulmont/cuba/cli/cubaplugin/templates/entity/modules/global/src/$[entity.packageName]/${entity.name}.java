package ${entity.packageName};

#if( ${entity.type} == "Persistent" )
import com.haulmont.cuba.core.entity.StandardEntity;
import javax.persistence.Entity;
import javax.persistence.Table;
#set( $superclass = "StandardEntity" )
#elseif (${entity.type} == "Persistent embedded" )
import javax.persistence.Embeddable;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.cuba.core.entity.EmbeddableEntity;
#set( $superclass = "EmbeddableEntity" )
#else
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
#set( $superclass = "BaseUuidEntity" )
#end

#if( ${entity.type} == "Persistent" )
@Table(name = "${entity.tableName}")
@Entity(name = "${project.namespace}${entity.sep}${entity.name}")
#elseif (${entity.type} == "Persistent embedded")
@MetaClass(name = "${project.namespace}${entity.sep}${entity.name}")
@Embeddable
#else
@MetaClass(name = "${project.namespace}${entity.sep}${entity.name}")
#end
public class ${entity.name} extends $superclass {
    private static final long serialVersionUID = 6323743611817286101L;

}