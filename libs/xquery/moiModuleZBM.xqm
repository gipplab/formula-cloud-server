xquery version "3.0";

(:~
: User: andre
: Date: 09.06.20
: Time: 18:06
: To change this template use File | Settings | File Templates.
:)

module namespace moizbm = "http://www.moi.org/zbm";

declare namespace mws = "http://search.mathweb.org/ns";
declare default element namespace "http://www.w3.org/1998/Math/MathML";

(:
declare namespace mws = "http://search.mathweb.org/ns";
declare default element namespace "http://www.w3.org/1998/Math/MathML";
:)

declare function moizbm:max-depth($root as node()?)
as xs:integer? {
    if ($root/*) then
        max($root/*/moizbm:max-depth(.)) + 1
    else
        1
};

(:
Convert a node to a string representation
Use buildString to generate correct conversions

HERE IS THE INVISIBLE TIMES CHARACTER: "⁢"
HERE IS THE FUNCTION APPLICATION CHARACTER: "⁡"
AND HERE NOT: ""
:)
declare function moizbm:convertToString($node as node()) as xs:string {
    let $txt :=
        if ($node/text()="⁢") then "ivt"
        else if ($node/text() = "⁡") then "fap"
        else $node/text()
    let $str := if(not($node/*)) then
        $node/name() || ":" || $txt
    else
    (: otherwise convert all child elements to string and combine the strings :)
        let $list := for $child in $node/*
        return moizbm:convertToString($child)
        return $node/name() || "(" || string-join($list, ",") || ")"
    return $str
};

(:
Given a sequence of nodes, this function calculate distributions
of its subtrees (regarding the mi elements in the given sequence)
:)
declare function moizbm:extractTerms(
        $docs as node()*,
        $minDocFreq as xs:integer
){
    for $doc in $docs
        let $docID := $doc/@data-doc-id
        for $expr in $doc/mws:expr
            let $fID := $expr/@url
            for $elements in $expr//math/*
                for $descendant in $elements/descendant-or-self::*[descendant::mi or name()="mi"]
                let $descendantDepth := moizbm:max-depth($descendant)
                let $str := moizbm:convertToString($descendant)
                group by $str
                let $fIDList := distinct-values($fID)
                let $num := count($descendant)
                where $num >= $minDocFreq
                order by $descendantDepth[1], $num descending
                return
                    <element freq="{$num}" depth="{$descendantDepth[1]}"
                    fIDs="{$fIDList}" docIDs="{$docID[1]}">
                        {$str}
                    </element>
};
