package ca.uhn.fhir.model.api;


public class UndeclaredExtension extends BaseElement {

	private String myUrl;
	private IElement myValue;

	public UndeclaredExtension() {
		super();
	}
	
	public UndeclaredExtension(String theUrl) {
		myUrl=theUrl;
	}

	public String getUrl() {
		return myUrl;
	}

	public IElement getValue() {
		return myValue;
	}

	public void setUrl(String theUrl) {
		myUrl = theUrl;
	}

	public void setValue(IElement theValue) {
		myValue = theValue;
	}

	@Override
	public boolean isEmpty() {
		return myValue == null || myValue.isEmpty();
	}

}