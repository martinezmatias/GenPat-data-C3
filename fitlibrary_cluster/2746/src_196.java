package fitlibraryGeneric.specify.genericFinder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import fitlibrary.object.DomainFixtured;
import fitlibrary.traverse.DomainAdapter;

@SuppressWarnings("unchecked")
public class GenericAndNonGenericFinder implements DomainAdapter, DomainFixtured {
	@SuppressWarnings("unused")
	private Pair<Integer,Integer> integerIntegerPair;
	
	public void setIntegerIntegerPair(Pair<Integer,Integer> pair) {
		this.integerIntegerPair = pair;
	}
	public Pair findPair(@SuppressWarnings("unused") String key) {
		return null;
	}
	public Pair findPair(@SuppressWarnings("unused") String key, Type type) {
		if (type instanceof ParameterizedType && ((ParameterizedType)type).getActualTypeArguments()[0] == Integer.class)
			return new Pair<Integer,Integer>(1,2);
		return null;
	}
	public Object getSystemUnderTest() {
		return null;
	}
}
