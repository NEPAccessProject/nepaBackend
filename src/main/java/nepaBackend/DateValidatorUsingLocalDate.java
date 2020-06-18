package nepaBackend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateValidatorUsingLocalDate implements DateValidator {
    private DateTimeFormatter dateFormatter;
     
    public DateValidatorUsingLocalDate(DateTimeFormatter dateFormatter) {
        this.dateFormatter = dateFormatter;
    }
 
    @Override
    public boolean isValid(String dateStr) {
        try {
            LocalDate.parse(dateStr, this.dateFormatter);
        } catch (DateTimeParseException e) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean isValidFormat( String value) {
        LocalDateTime ldt = null;

        try {
            ldt = LocalDateTime.parse(value, this.dateFormatter);
            String result = ldt.format(this.dateFormatter);
            return result.equals(value);
        } catch (DateTimeParseException e) {
            try {
                LocalDate ld = LocalDate.parse(value, this.dateFormatter);
                String result = ld.format(this.dateFormatter);
                return result.equals(value);
            } catch (DateTimeParseException exp) {
                try {
                    LocalTime lt = LocalTime.parse(value, this.dateFormatter);
                    String result = lt.format(this.dateFormatter);
                    return result.equals(value);
                } catch (DateTimeParseException e2) {
                    // Debugging purposes
                    //e2.printStackTrace();
                }
            }
        }

        return false;
    }
}