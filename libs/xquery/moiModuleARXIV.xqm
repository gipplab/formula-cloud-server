xquery version "3.0";

(:~
: User: andre
: Date: 11.06.20
: Time: 14:21
: To change this template use File | Settings | File Templates.
:)

module namespace moiarxiv = "http://www.moi.org/arxiv";

declare namespace mws = "http://search.mathweb.org/ns";
declare default element namespace "http://www.w3.org/1998/Math/MathML";

(:
declare namespace mws = "http://search.mathweb.org/ns";
declare default element namespace "http://www.w3.org/1998/Math/MathML";
:)

declare function moiarxiv:max-depth($root as node()?)
as xs:integer? {
    if ($root/*) then
        max($root/*/moiarxiv:max-depth(.)) + 1
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
declare function moiarxiv:convertToString($node as node()) as xs:string {
    let $txt :=
        if ($node/text()="⁢") then "ivt"
        else if ($node/text() = "⁡") then "fap"
        else $node/text()
    let $str := if(not($node/*)) then
        $node/name() || ":" || $txt
    else
    (: otherwise convert all child elements to string and combine the strings :)
        let $list := for $child in $node/*
        return moiarxiv:convertToString($child)
        return $node/name() || "(" || string-join($list, ",") || ")"
    return $str
};

(:
Given a sequence of nodes, this function calculate distributions
of its subtrees (regarding the mi elements in the given sequence)
:)
declare function moiarxiv:extractTerms(
        $docs as node()*,
        $minDocFreq as xs:integer
){
    for $doc in $docs
        let $docID := $doc/@data-doc-id
        for $expr in $doc/mws:expr
            for $elements in $expr//math/*
                for $descendant in $elements/descendant-or-self::*[descendant::mi or name()="mi"]
                    let $descendantDepth := moiarxiv:max-depth($descendant)
                    let $str := moiarxiv:convertToString($descendant)
                    group by $str
                    let $num := count($descendant)
                    where $num >= $minDocFreq
                    order by $descendantDepth[1], $num descending
                    return
                        <element freq="{$num}" depth="{$descendantDepth[1]}"
                        fIDs="" docIDs="{$docID[1]}">
                            {$str}
                        </element>
};
