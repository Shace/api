package Utils;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import play.db.ebean.Model;
import flexjson.ChainedSet;
import flexjson.Path;
import flexjson.TypeContext;
import flexjson.transformer.AbstractTransformer;


public class ModelTransformer extends AbstractTransformer {

	@Override
	public void transform(Object obj) {
		Class<? extends Object> currentClass = obj.getClass();
		if (!Model.class.isAssignableFrom(currentClass)) {
			return;
		}
		TypeContext typeContext = getContext().writeOpenObject();
		Method[] methods = currentClass.getMethods();

		for (Method method : methods) {
			JSONable annotation = method.getAnnotation(JSONable.class);
			if (annotation != null && annotation.defaultField()) {
				try {
					String name = annotation.name();
					if (name == null || name.isEmpty()) {
						name = method.getName();
					}
					if (!typeContext.isFirst())
						getContext().writeComma();
					else
						typeContext.setFirst(false);
					getContext().writeName(name);
					getContext().transform(method.invoke(obj));
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				} catch (InvocationTargetException e) {
				}
			}
		}
		Field[] fields = currentClass.getFields();

		for (Field field : fields) {
			JSONable annotation = field.getAnnotation(JSONable.class);
			if (annotation != null && annotation.defaultField()) {
				try {
					String name = annotation.name();
					if (name == null || name.isEmpty()) {
						name = field.getName();
					}
					if (!typeContext.isFirst())
						getContext().writeComma();
					else
						typeContext.setFirst(false);
					getContext().writeName(name);
					getContext().transform(field.get(obj));
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				}
			}
		}
		getContext().writeCloseObject();
	}
}
