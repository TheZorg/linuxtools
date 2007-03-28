package org.eclipse.cdt.rpm.editor;

import org.eclipse.swt.graphics.RGB;

public interface ISpecfileColorConstants {
	// general constants 
	RGB DEFAULT = new RGB(0, 0, 0);
	RGB SECTIONS = new RGB(128, 0, 0);
	RGB MACROS = new RGB(0, 0, 128);
	RGB KEYWORDS = new RGB(127, 0, 85);
	RGB DEFINES = new RGB(0, 128, 0);
	RGB TAGS = new RGB(255, 101, 52);
	RGB COMMENT = new RGB(63, 95, 191);
	
	// changelog section specific constants
	RGB AUTHOR_MAIL = new RGB(10, 20, 175);
	RGB VER_REL = new RGB(255, 101, 52);
	
	
}
