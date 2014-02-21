package ca.uhn.fhir.starter;

import static org.apache.commons.lang.StringUtils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ch.qos.logback.core.db.dialect.MySQLDialect;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.ResourceReference;
import ca.uhn.fhir.model.datatype.CodeDt;
import ca.uhn.fhir.model.datatype.CodeableConceptDt;
import ca.uhn.fhir.starter.model.BaseElement;
import ca.uhn.fhir.starter.model.Child;
import ca.uhn.fhir.starter.model.Resource;
import ca.uhn.fhir.starter.model.ResourceBlock;
import ca.uhn.fhir.starter.util.XMLUtils;

public abstract class BaseParser {

	private String myDirectory;
	private String myOutputFile;
	private int myColName;
	private int myColCard;
	private int myColType;
	private int myColBinding;
	private int myColShortName;
	private int myColDefinition;
	private int myColV2Mapping;
	private int myColRequirements;
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BaseParser.class);

	public void parse() throws Exception {
		File baseDir = new File(myDirectory);
		if (baseDir.exists() == false || baseDir.isDirectory() == false) {
			throw new Exception(myDirectory + " does not exist or is not a directory");
		}

		File resourceSpreadsheetFile = new File(baseDir, getFilename());
		if (resourceSpreadsheetFile.exists() == false) {
			throw new Exception(resourceSpreadsheetFile.getAbsolutePath() + " does not exist");
		}

		Document file = XMLUtils.parse(new FileInputStream(resourceSpreadsheetFile), false);
		Element dataElementsSheet = (Element) file.getElementsByTagName("Worksheet").item(0);
		NodeList tableList = dataElementsSheet.getElementsByTagName("Table");
		Element table = (Element) tableList.item(0);

		NodeList rows = table.getElementsByTagName("Row");

		Element defRow = (Element) rows.item(0);
		parseFirstRow(defRow);

		Element resourceRow = (Element) rows.item(1);
		Resource resource = new Resource();
		parseBasicElements(resourceRow, resource);

		Map<String, BaseElement> elements = new HashMap<String, BaseElement>();
		elements.put(resource.getElementName(), resource);

		for (int i = 2; i < rows.getLength(); i++) {
			Element nextRow = (Element) rows.item(i);
			String name = cellValue(nextRow, 0);
			if (name == null || name.startsWith("!")) {
				continue;
			}

			String type = cellValue(nextRow, myColType);

			Child elem;
			if (StringUtils.isBlank(type) || type.startsWith("=")) {
				elem = new ResourceBlock();
			} else {
				elem = new Child();
			}

			parseBasicElements(nextRow, elem);

			if (elem.isResourceRef()) {
				elem.setReferenceType(ResourceReference.class.getSimpleName());
			} else if (elem.getType().size() == 1) {
				String elemName = elem.getType().get(0);
				elemName = elemName.substring(0, 1).toUpperCase() + elemName.substring(1);
				if (elem instanceof ResourceBlock) {
					elem.setReferenceType(elemName);
				} else {
					elem.setReferenceType(elemName + "Dt");
				}

				// if
				// (elem.getReferenceType().equals(CodeDt.class.getSimpleName())
				// ||
				// elem.getReferenceType().equals(CodeableConceptDt.class.getSimpleName()))
				// {
				// elem.setReferenceType(elemName + "Dt<" + elem.getBinding() +
				// "Enum>");
				// }

			} else {
				elem.setReferenceType(IDatatype.class.getSimpleName());
			}

			if (elem.isRepeatable()) {
				elem.setReferenceType("List<" + elem.getReferenceType() + ">");
			}

			for (int childIdx = 0; childIdx < elem.getType().size(); childIdx++) {
				String nextType = elem.getType().get(childIdx);
				if (elem.isResourceRef()) {
					nextType = nextType.substring(0, 1).toUpperCase() + nextType.substring(1);
				} else {
					nextType = nextType.substring(0, 1).toUpperCase() + nextType.substring(1) + "Dt";
				}
				elem.getType().set(childIdx, nextType);
			}

			elements.put(elem.getName(), elem);
			BaseElement parent = elements.get(elem.getElementParentName());
			if (parent == null) {
				throw new Exception("Can't find element " + elem.getElementParentName() + "  -  Valid values are: " + elements.keySet());
			}
			parent.getChildren().add(elem);

		}

		write(resource);

	}

	public void setDirectory(String theDirectory) {
		myDirectory = theDirectory;
	}

	public void setOutputFile(String theOutputFile) {
		myOutputFile = theOutputFile;
	}

	static String cellValue(Node theRowXml, int theCellIndex) {
		NodeList cells = ((Element) theRowXml).getElementsByTagName("Cell");

		for (int i = 0, currentCell = 0; i < cells.getLength(); i++) {
			Element nextCell = (Element) cells.item(i);
			String indexVal = nextCell.getAttributeNS("urn:schemas-microsoft-com:office:spreadsheet", "Index");
			if (StringUtils.isNotBlank(indexVal)) {
				// 1-indexed for some reason...
				currentCell = Integer.parseInt(indexVal) - 1;
			}

			if (currentCell == theCellIndex) {
				NodeList dataElems = nextCell.getElementsByTagName("Data");
				Element dataElem = (Element) dataElems.item(0);
				if (dataElem == null) {
					return null;
				}
				String retVal = dataElem.getTextContent();
				return retVal;
			}

			currentCell++;
		}

		return null;
	}

	private void write(Resource theResource) throws IOException {
		File f = new File(myOutputFile);
		FileWriter w = new FileWriter(f, false);

		ourLog.info("Writing file: {}", f.getAbsolutePath());

		VelocityContext ctx = new VelocityContext();
		ctx.put("className", theResource.getName());
		ctx.put("shortName", defaultString(theResource.getShortName()));
		ctx.put("definition", defaultString(theResource.getDefinition()));
		ctx.put("requirements", defaultString(theResource.getRequirement()));
		ctx.put("children", theResource.getChildren());
		ctx.put("resourceBlockChildren", theResource.getResourceBlockChildren());

		VelocityEngine v = new VelocityEngine();
		v.setProperty("resource.loader", "cp");
		v.setProperty("cp.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

		InputStream templateIs = ResourceParser.class.getResourceAsStream(getTemplate());
		InputStreamReader templateReader = new InputStreamReader(templateIs);
		v.evaluate(ctx, w, "", templateReader);

		w.close();
	}

	protected abstract String getFilename();

	protected abstract String getTemplate();

	private void parseFirstRow(Element theDefRow) {
		for (int i = 0; i < 20; i++) {
			String nextName = cellValue(theDefRow, i);
			if (nextName == null) {
				continue;
			}
			nextName = nextName.toLowerCase().trim().replace(".", "");
			if ("element".equals(nextName)) {
				myColName = i;
			} else if ("card".equals(nextName)) {
				myColCard = i;
			} else if ("type".equals(nextName)) {
				myColType = i;
			} else if ("binding".equals(nextName)) {
				myColBinding = i;
			} else if ("short name".equals(nextName)) {
				myColShortName = i;
			} else if ("definition".equals(nextName)) {
				myColDefinition = i;
			} else if ("requirements".equals(nextName)) {
				myColRequirements = i;
			} else if ("v2 mapping".equals(nextName)) {
				myColV2Mapping = i;
			}
		}
	}

	protected void parseBasicElements(Element theRowXml, BaseElement theTarget) {
		String name = cellValue(theRowXml, myColName);
		theTarget.setName(name);

		int lastDot = name.lastIndexOf('.');
		if (lastDot == -1) {
			theTarget.setElementName(name);
		} else {
			String elementName = name.substring(lastDot + 1);
			String elementParentName = name.substring(0, lastDot);
			theTarget.setElementName(elementName);
			theTarget.setElementParentName(elementParentName);
		}

		String cardValue = cellValue(theRowXml, myColCard);
		if (cardValue != null && cardValue.contains("..")) {
			String[] split = cardValue.split("\\.\\.");
			theTarget.setCardMin(split[0]);
			theTarget.setCardMax(split[1]);
		}

		String type = cellValue(theRowXml, myColType);
		theTarget.setTypeFromString(type);

		theTarget.setBinding(cellValue(theRowXml, myColBinding));
		theTarget.setShortName(cellValue(theRowXml, myColShortName));
		theTarget.setDefinition(cellValue(theRowXml, myColDefinition));
		theTarget.setRequirement(cellValue(theRowXml, myColRequirements));
		theTarget.setV2Mapping(cellValue(theRowXml, myColV2Mapping));
	}

}