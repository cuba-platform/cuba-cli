package ${screen.packageName};

import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import ${screen.entity.fqn};

@UiController("${screen.screenId}")
@UiDescriptor("${screen.descriptorName}.xml")
@EditedEntityContainer("${screen.entity.className.toLowerCase()}Dc")
@LoadDataBeforeShow
public class ${screen.controllerName} extends StandardEditor<${screen.entity.className}> {
}