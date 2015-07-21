package bt.adapt;

import jnibwapi.types.OrderType;
import jnibwapi.types.UnitCommandType;
import jnibwapi.types.UnitType;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByIdMarshaller;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.mapper.Mapper;

public class NameAsIdReferenceMarshallingStrategy extends ReferenceByIdMarshallingStrategy {
	
	@Override
	protected TreeMarshaller createMarshallingContext(
			HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper) {
		return new ReferenceByNameIdMarshaller(writer, converterLookup, mapper);
	}
	
	public static class ReferenceByNameIdMarshaller extends ReferenceByIdMarshaller {
		
		public ReferenceByNameIdMarshaller(HierarchicalStreamWriter writer,
				ConverterLookup converterLookup,
				Mapper mapper,
				IDGenerator idGenerator) {
			super(writer, converterLookup, mapper, idGenerator);
		}
		
		public ReferenceByNameIdMarshaller(HierarchicalStreamWriter writer,
				ConverterLookup converterLookup,
				Mapper mapper) {
			super(writer, converterLookup, mapper);
		}
		
		protected Object createReferenceKey(Path currentPath, Object item) {
			if (item instanceof UnitType) {
				return "UnitType:" + ((UnitType) item).getName();
			} else if (item instanceof OrderType) {
				if (((OrderType) item).getName() == null)
					return "OrderType:" + ((OrderType) item).getID();
				else
					return "OrderType:" + ((OrderType) item).getName();
			} else if (item instanceof UnitCommandType) {
				return "UnitCommandType:" + ((UnitCommandType) item).getName();
			} else {
				return super.createReferenceKey(currentPath, item);
			}
		}
		
	}
}
