package bt.adapt;

import jnibwapi.types.UnitCommandType;
import jnibwapi.types.UnitCommandType.UnitCommandTypes;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class UnitCommandTypeConverter implements Converter {
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return type.equals(UnitCommandType.class);
	}
	
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		UnitCommandType t = (UnitCommandType)source;
		writer.addAttribute("unitCommandTypeId", String.valueOf(t.getID()));
	}
	
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		int id = Integer.parseInt(reader.getAttribute("unitCommandTypeId"));
		UnitCommandType t = UnitCommandTypes.getUnitCommandType(id);
		return t;
	}
	
}
