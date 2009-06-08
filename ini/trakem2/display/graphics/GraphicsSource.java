package ini.trakem2.display.graphics;

import ini.trakem2.display.Displayable;
import ini.trakem2.display.Paintable;
import java.util.Collection;

public interface GraphicsSource {

	public Collection<? extends Paintable> asPaintable(final Collection<? extends Paintable> ds);

}