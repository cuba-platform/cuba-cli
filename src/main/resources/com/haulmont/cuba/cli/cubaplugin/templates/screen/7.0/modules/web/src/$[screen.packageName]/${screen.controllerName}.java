package ${screen.packageName};

import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("${screen.screenId}")
@UiDescriptor("${screen.descriptorName}.xml")
public class ${screen.controllerName} extends Screen {
}