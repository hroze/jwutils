/* This file was generated by SableCC (http://www.sablecc.org/). */

package jw.jzbot.eval.jexec.node;

import jw.jzbot.eval.jexec.analysis.*;

@SuppressWarnings("nls")
public final class AInUnmp extends PUnmp
{
    private TMinus _minus_;
    private PTerm _second_;

    public AInUnmp()
    {
        // Constructor
    }

    public AInUnmp(
        @SuppressWarnings("hiding") TMinus _minus_,
        @SuppressWarnings("hiding") PTerm _second_)
    {
        // Constructor
        setMinus(_minus_);

        setSecond(_second_);

    }

    @Override
    public Object clone()
    {
        return new AInUnmp(
            cloneNode(this._minus_),
            cloneNode(this._second_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAInUnmp(this);
    }

    public TMinus getMinus()
    {
        return this._minus_;
    }

    public void setMinus(TMinus node)
    {
        if(this._minus_ != null)
        {
            this._minus_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._minus_ = node;
    }

    public PTerm getSecond()
    {
        return this._second_;
    }

    public void setSecond(PTerm node)
    {
        if(this._second_ != null)
        {
            this._second_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._second_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._minus_)
            + toString(this._second_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._minus_ == child)
        {
            this._minus_ = null;
            return;
        }

        if(this._second_ == child)
        {
            this._second_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._minus_ == oldChild)
        {
            setMinus((TMinus) newChild);
            return;
        }

        if(this._second_ == oldChild)
        {
            setSecond((PTerm) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}
