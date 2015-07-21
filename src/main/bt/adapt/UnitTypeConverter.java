package bt.adapt;

import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class UnitTypeConverter implements Converter {
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return type.equals(UnitType.class);
	}
	
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		UnitType t = (UnitType)source;
		writer.addAttribute("unitTypeId", String.valueOf(t.getID()));
	}
	
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		int id = Integer.parseInt(reader.getAttribute("unitTypeId"));
		UnitType t = UnitTypes.getUnitType(id);
		return t;
	}
	
}
