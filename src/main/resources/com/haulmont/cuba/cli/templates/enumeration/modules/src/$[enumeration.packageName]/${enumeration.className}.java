package ${enumaration.packageName};
#set( $idClass = ${enumeration.idType} )

import com.haulmont.chile.core.datatypes.impl.EnumClass;
import javax.annotation.Nullable;

public enum ${enumeration.className} implements EnumClass<$idClass> {

#foreach( $value in ${enumeration.values} )
#if( $foreach.index == $enumeration.values.size() - 1 )
#set( $lineEnd = ";" )
#else
#set( $lineEnd = "," )
#end
#if( $idClass == "String" )
    ${value.name}("$value.id")$lineEnd
#else
    ${value.name}($value.id)$lineEnd
#end
#end
#if( $enumeration.values.size() == 0)
    ;
#end

    private $idClass id;

    NewEnum($idClass value) {
        this.id = value;
    }

    public $idClass getId() {
        return id;
    }

    @Nullable
    public static NewEnum fromId($idClass id) {
        for (NewEnum at : NewEnum.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}