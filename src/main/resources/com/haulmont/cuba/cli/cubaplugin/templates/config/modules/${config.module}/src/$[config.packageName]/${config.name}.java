package ${config.packageName};

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;

@Source(type = SourceType.${config.sourceType})
public interface ${config.name} extends Config {
}