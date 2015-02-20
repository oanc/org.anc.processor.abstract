package org.anc.processor.Abstract.i18n

import org.anc.i18n.*
import org.anc.i18n.BaseTranslation.Default

/**
 * Created by danmccormack on 2/19/15.
 */
class Messages extends BaseTranslation
{
    @Default("Invalid document ID.")
    final String INVALID_ID

    @Default("Invalid annotation type(s) selected.")
    final String INVALID_TYPE

    @Default("No annotation type(s) provided")
    final String NO_ANNOTATIONS

    @Default("No document id provided.")
    final String NO_DOC_ID

    public Messages() {
        super()
        init()
    }
}

