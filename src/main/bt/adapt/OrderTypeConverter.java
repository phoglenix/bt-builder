package bt.adapt;

import jnibwapi.types.OrderType;
import jnibwapi.types.OrderType.OrderTypes;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class OrderTypeConverter implements Converter {
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return type.equals(OrderType.class);
	}
	
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		OrderType t = (OrderType)source;
		writer.addAttribute("orderTypeId", String.valueOf(t.getID()));
	}
	
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		int id = Integer.parseInt(reader.getAttribute("orderTypeId"));
		OrderType t = OrderTypes.getOrderType(id);
		return t;
	}
	
}
