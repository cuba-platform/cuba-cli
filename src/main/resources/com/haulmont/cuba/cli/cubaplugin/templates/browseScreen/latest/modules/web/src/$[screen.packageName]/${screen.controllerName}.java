#set( $entities = "${screen.entity.className.toLowerCase()}s")
package ${screen.packageName};

import ${screen.entity.fqn};
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("${screen.screenId}")
@UiDescriptor("${screen.descriptorName}.xml")
@LookupComponent("${entities}Table")
public class ${screen.controllerName} extends StandardLookup<${screen.entity.className}> {
}