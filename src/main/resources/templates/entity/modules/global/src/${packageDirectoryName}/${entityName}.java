package $packageName;

#if( ${entity.Type} == "Persistent" )
import com.haulmont.cuba.core.entity.StandardEntity;
import javax.persistence.Entity;
import javax.persistence.Table;
#set( $superclass = "StandardEntity" )
#elseif (${entity.Type} == "Persistent embedded" )
import javax.persistence.Embeddable;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.cuba.core.entity.EmbeddableEntity;
#set( $superclass = "EmbeddableEntity" )
#else
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
#set( $superclass = "BaseUuidEntity" )
#end

#if( ${entity.Type} == "Persistent" )
@Table(name = "${entity.TableName}")
@Entity(name = "${project.Namespace}$${entity.Name}")
#elseif (${entity.Type} == "Persistent embedded")
@MetaClass(name = "${project.Namespace}$${entity.Name}")
@Embeddable
#else
@MetaClass(name = "${project.Namespace}$${entity.Name}")
#end
public class ${entity.Name} extends $superclass {
    private static final long serialVersionUID = 6323743611817286101L;

}