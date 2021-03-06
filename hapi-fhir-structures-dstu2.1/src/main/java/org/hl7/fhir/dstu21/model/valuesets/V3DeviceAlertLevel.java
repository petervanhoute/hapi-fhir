package org.hl7.fhir.dstu21.model.valuesets;

import org.hl7.fhir.dstu21.exceptions.FHIRException;

public enum V3DeviceAlertLevel {

        /**
         * Shut Down, Fix Problem and Re-initialize
         */
        C, 
        /**
         * No Corrective Action Needed
         */
        N, 
        /**
         * Corrective Action Required
         */
        S, 
        /**
         * Corrective Action Anticipated
         */
        W, 
        /**
         * added to help the parsers
         */
        NULL;
        public static V3DeviceAlertLevel fromCode(String codeString) throws FHIRException {
            if (codeString == null || "".equals(codeString))
                return null;
        if ("C".equals(codeString))
          return C;
        if ("N".equals(codeString))
          return N;
        if ("S".equals(codeString))
          return S;
        if ("W".equals(codeString))
          return W;
        throw new FHIRException("Unknown V3DeviceAlertLevel code '"+codeString+"'");
        }
        public String toCode() {
          switch (this) {
            case C: return "C";
            case N: return "N";
            case S: return "S";
            case W: return "W";
            default: return "?";
          }
        }
        public String getSystem() {
          return "http://hl7.org/fhir/v3/DeviceAlertLevel";
        }
        public String getDefinition() {
          switch (this) {
            case C: return "Shut Down, Fix Problem and Re-initialize";
            case N: return "No Corrective Action Needed";
            case S: return "Corrective Action Required";
            case W: return "Corrective Action Anticipated";
            default: return "?";
          }
        }
        public String getDisplay() {
          switch (this) {
            case C: return "Critical";
            case N: return "Normal";
            case S: return "Serious";
            case W: return "Warning";
            default: return "?";
          }
    }


}

