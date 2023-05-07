package ass.parser;

import ass.model.IAssSection;

abstract class ISectionParser {

    abstract void header(String line);

    abstract void parseLine(String line);

    abstract IAssSection build();
}
