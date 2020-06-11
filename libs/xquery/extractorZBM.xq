xquery version "3.0";

(:~
: User: andre
: Date: 09.06.20
: Time: 18:08
: To change this template use File | Settings | File Templates.
:)

import module namespace moi = "http://www.moi.org/zbm" at "libs/xquery/moiModuleZBM.xqm";

declare namespace mws = "http://search.mathweb.org/ns";
declare default element namespace "http://www.w3.org/1998/Math/MathML";

declare variable $docid external;
declare variable $minDocFreq := 1;
declare variable $docs := /mws:harvest[@data-doc-id=$docid];

moi:extractTerms($docs, $minDocFreq)