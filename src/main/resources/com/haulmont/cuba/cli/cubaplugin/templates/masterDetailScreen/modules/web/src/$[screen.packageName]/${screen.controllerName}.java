package ${screen.packageName};

import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.MasterDetailScreen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import ${screen.entity.fqn};

@UiController("${screen.screenId}")
@UiDescriptor("${screen.descriptorName}.xml")
@LookupComponent("table")
@LoadDataBeforeShow
public class ${screen.controllerName} extends MasterDetailScreen<${screen.entity.className}> {
}