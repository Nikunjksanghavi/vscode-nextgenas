/*
Copyright 2016-2017 Bowler Hat LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.nextgenactionscript.vscode.utils;

import org.apache.royale.compiler.definitions.IClassDefinition;
import org.apache.royale.compiler.definitions.IDefinition;
import org.apache.royale.compiler.internal.projects.RoyaleProject;
import org.apache.royale.compiler.mxml.IMXMLLanguageConstants;
import org.apache.royale.compiler.mxml.IMXMLTagAttributeData;
import org.apache.royale.compiler.mxml.IMXMLTagData;

public class MXMLDataUtils
{
    private static final String[] LANGUAGE_TYPE_NAMES =
	{
		IMXMLLanguageConstants.ARRAY,
		IMXMLLanguageConstants.BOOLEAN,
		IMXMLLanguageConstants.CLASS,
		IMXMLLanguageConstants.DATE,
		IMXMLLanguageConstants.FUNCTION,
		IMXMLLanguageConstants.INT,
		IMXMLLanguageConstants.NUMBER,
		IMXMLLanguageConstants.OBJECT,
		IMXMLLanguageConstants.STRING,
		IMXMLLanguageConstants.XML,
		IMXMLLanguageConstants.XML_LIST,
		IMXMLLanguageConstants.UINT
	};

	public static boolean isInsideTagPrefix(IMXMLTagData tag, int offset)
    {
        //next, check that we're after the prefix
        //one extra for bracket
        int maxOffset = tag.getAbsoluteStart() + 1;
        String prefix = tag.getPrefix();
        int prefixLength = prefix.length();
        if (prefixLength > 0)
        {
            //one extra for colon
            maxOffset += prefixLength + 1;
        }
        return offset > tag.getAbsoluteStart() && offset < maxOffset;
    }

    public static boolean isDeclarationsTag(IMXMLTagData tag)
    {
        if (tag == null)
        {
            return false;
        }
        String shortName = tag.getShortName();
        if (shortName == null || !shortName.equals(IMXMLLanguageConstants.DECLARATIONS))
        {
            return false;
        }
        String uri = tag.getURI();
        if (uri == null || !uri.equals(IMXMLLanguageConstants.NAMESPACE_MXML_2009))
        {
            return false;
        }
        return true;
    }

    public static IMXMLTagAttributeData getMXMLTagAttributeAtOffset(IMXMLTagData tag, int offset)
    {
        IMXMLTagAttributeData[] attributes = tag.getAttributeDatas();
        for (IMXMLTagAttributeData attributeData : attributes)
        {
            if (offset >= attributeData.getAbsoluteStart()
                    && offset <= attributeData.getValueEnd())
            {
                return attributeData;
            }
        }
        return null;
    }

    public static IMXMLTagAttributeData getMXMLTagAttributeWithNameAtOffset(IMXMLTagData tag, int offset, boolean includeEnd)
    {
        IMXMLTagAttributeData[] attributes = tag.getAttributeDatas();
        for (IMXMLTagAttributeData attributeData : attributes)
        {
            if (offset >= attributeData.getAbsoluteStart())
            {
                if(includeEnd && offset <= attributeData.getAbsoluteEnd())
                {
                    return attributeData;
                }
                else if(offset < attributeData.getAbsoluteEnd())
                {
                    return attributeData;
                }
            }
        }
        return null;
    }

    public static IMXMLTagAttributeData getMXMLTagAttributeWithValueAtOffset(IMXMLTagData tag, int offset)
    {
        IMXMLTagAttributeData[] attributes = tag.getAttributeDatas();
        for (IMXMLTagAttributeData attributeData : attributes)
        {
            if (offset >= attributeData.getValueStart()
                    && offset <= attributeData.getValueEnd())
            {
                return attributeData;
            }
        }
        return null;
    }

    public static IDefinition getDefinitionForMXMLTag(IMXMLTagData tag, RoyaleProject project)
    {
        if (tag == null)
        {
            return null;
        }

        IDefinition offsetDefinition = project.resolveXMLNameToDefinition(tag.getXMLName(), tag.getMXMLDialect());
        if (offsetDefinition != null)
        {
            return offsetDefinition;
        }
        if (tag.getXMLName().getXMLNamespace().equals(tag.getMXMLDialect().getLanguageNamespace()))
        {
            for (String typeName : LANGUAGE_TYPE_NAMES)
            {
                if (tag.getShortName().equals(typeName))
                {
                    return project.resolveQNameToDefinition(typeName);
                }
            }
        }
        IMXMLTagData parentTag = tag.getParentTag();
        if (parentTag == null)
        {
            return null;
        }
        IDefinition parentDefinition = project.resolveXMLNameToDefinition(parentTag.getXMLName(), parentTag.getMXMLDialect());
        if (parentDefinition == null || !(parentDefinition instanceof IClassDefinition))
        {
            return null;
        }
        IClassDefinition classDefinition = (IClassDefinition) parentDefinition;
        return project.resolveSpecifier(classDefinition, tag.getShortName());
    }

    public static IDefinition getDefinitionForMXMLTagAttribute(IMXMLTagData tag, int offset, boolean includeValue, RoyaleProject project)
    {
        IMXMLTagAttributeData attributeData = null;
        if (includeValue)
        {
            attributeData = getMXMLTagAttributeAtOffset(tag, offset);
        }
        else
        {
            attributeData = getMXMLTagAttributeWithNameAtOffset(tag, offset, false);
        }
        if (attributeData == null)
        {
            return null;
        }
        IDefinition tagDefinition = getDefinitionForMXMLTag(tag, project);
        if (tagDefinition != null
                && tagDefinition instanceof IClassDefinition)
        {
            IClassDefinition classDefinition = (IClassDefinition) tagDefinition;
            return project.resolveSpecifier(classDefinition, attributeData.getShortName());
        }
        return null;
    }

    public static IDefinition getDefinitionForMXMLNameAtOffset(IMXMLTagData tag, int offset, RoyaleProject project)
    {
        if (tag.isOffsetInAttributeList(offset))
        {
            return getDefinitionForMXMLTagAttribute(tag, offset, false, project);
        }
        return getDefinitionForMXMLTag(tag, project);
    }
}