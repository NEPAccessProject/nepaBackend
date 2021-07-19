package nepaBackend;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Comparator;
import java.util.regex.Pattern;


class NameComparator implements Comparator<String> {

    private Pattern coverPattern = Pattern.compile("^(?!.*(app|map)).*(front matter|cover).*$");
    private Pattern tocPattern = Pattern.compile("^(?!.*(app|map)).*(table of contents|toc).*$");
    private Pattern summaryPattern = Pattern.compile("^(?!.*(app|map)).*summary.*$");
    private Pattern eisPattern = Pattern.compile("^(?!.*(app|map)).*eis.*$");
    private Pattern volumePattern = Pattern.compile("^(?!.*(app|map)).*(volume|vol).*$");
    private Pattern partPattern = Pattern.compile("^(?!.*(app|map)).*(part).*$");
    private Pattern chapterPattern = Pattern.compile("^(?!.*(app|map)).*(chapter|chpt).*$");
    private Pattern sectionPattern = Pattern.compile("^(?!.*(app|map)).*(section).*$");
    private Pattern appendixPattern = Pattern.compile("(\bapp\b|appendix|appendices)");
    private Pattern mapPattern = Pattern.compile("(\bmap\b|\bmaps\b)");
    private Pattern noiPattern = Pattern.compile("(\bnoi\b|notice of intent)");
    private Pattern noaPattern = Pattern.compile("(\bnoa\b|notice of availability)");
    private Pattern rodPattern = Pattern.compile("(\brod\b|record of decision)");


    public int compare(String vA, String vB) {
        return nameComparison(vA, vB);
    }

    private int nameComparison(String vA, String vB) {
        String preprocessedVA = preProcessString(vA);
        String preprocessedVB = preProcessString(vB);
        int comparisonResult = regexComparison(preprocessedVA, preprocessedVB);
        if (comparisonResult == 0)
            comparisonResult = naturalComparison(preprocessedVA, preprocessedVB);
        return comparisonResult;
    }

    private int regexComparison(String vA, String vB){
        return patternNumericValue(vA).compareTo(patternNumericValue(vB));
    }

    private int naturalComparison(String vA, String vB) {
        CharacterIterator vAIterable = new StringCharacterIterator(vA);
        CharacterIterator vBIterable = new StringCharacterIterator(vB);
        while (vAIterable.current() != CharacterIterator.DONE &&
                vBIterable.current() != CharacterIterator.DONE) {
            Character currentCharA = vAIterable.current();
            Character currentCharB = vBIterable.current();
            if (currentCharA.toString().equals(".") &&
                    currentCharB.toString().equals(".")) {
                vAIterable.next();
                vBIterable.next();
            } else if (currentCharA.toString().equals("."))
                return -1;
            else if (currentCharB.toString().equals("."))
                return 1;
            else if (Character.isDigit(currentCharA) &&
                    Character.isDigit(currentCharB)) {
                Integer intA = charsToNumber(vAIterable);
                Integer intB = charsToNumber(vBIterable);
                int comparison = intA.compareTo(intB);
                if (comparison != 0)
                    return comparison;
            } else if (Character.isDigit(currentCharA))
                return -1;
            else if (Character.isDigit(currentCharB))
                return 1;
            else {
                int comparison = currentCharA.compareTo(currentCharB);
                if (comparison != 0)
                    return comparison;
                else {
                    vAIterable.next();
                    vBIterable.next();
                }
            }
        }
        if (vBIterable.current() != CharacterIterator.DONE)
            return -1;
        else if (vAIterable.current() != CharacterIterator.DONE)
            return 1;
        else
            return 0;
    }

    private String preProcessString(String string) {
        string = string.toLowerCase().replaceAll("'", "").
                replaceAll(".pdf", "").
                replaceAll(" ", "_");
        return string;
    }

    private int charsToNumber(CharacterIterator charIterable) {
        StringBuilder digits = new StringBuilder();
        while(Character.isDigit(charIterable.current())) {
            digits.append(charIterable.current());
            charIterable.next();
        }
        return Integer.parseInt(digits.toString());
    }

    private Integer patternNumericValue(String string) {
        if (coverPattern.matcher(string).find())
            return 0;
        else if (tocPattern.matcher(string).find())
            return 1;
        else if (summaryPattern.matcher(string).find())
            return 2;
        else if (eisPattern.matcher(string).find())
            return 3;
        else if (volumePattern.matcher(string).find())
            return 4;
        else if (partPattern.matcher(string).find())
            return 5;
        else if (chapterPattern.matcher(string).find())
            return 6;
        else if (sectionPattern.matcher(string).find())
            return 7;
        else if (appendixPattern.matcher(string).find())
            return 8;
        else if (mapPattern.matcher(string).find())
            return 9;
        else if (noiPattern.matcher(string).find())
            return 11;
        else if (noaPattern.matcher(string).find())
            return 12;
        else if (rodPattern.matcher(string).find())
            return 13;
        else
            return 10;
    }
}