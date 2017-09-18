/*
 *  This file is part of the noOp organization .
 *
 *  (c) Cyrille Lebeaupin <clebeaupin@noop.fr>
 *
 *  For the full copyright and license information, please view the LICENSE
 *  file that was distributed with this source code.
 *
 */

package com.xc.mixsubtitle.subtitle.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by clebeaupin on 02/10/15.
 */
public interface SubtitleParser {
    public SubtitleObject parse(InputStream is) throws IOException, SubtitleParsingException;
    public SubtitleObject parse(InputStream is, boolean strict) throws IOException, SubtitleParsingException;
}
