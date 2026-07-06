package com.explorebyte.ar.presentation.chatbot

import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.explorebyte.ar.R

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    fun submitList(newList: List<Message>) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserViewHolder) holder.bind(message)
        else if (holder is AiViewHolder) holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size
    
    private fun formatMarkdown(text: String): Spanned {
        var formattedText = text.replace("\n", "<br>")
        // Bold
        formattedText = formattedText.replace(Regex("\\*\\*(.*?)\\*\\*")) { result ->
            "<b>${result.groupValues[1]}</b>"
        }
        // Italic
        formattedText = formattedText.replace(Regex("\\*(.*?)\\*")) { result ->
            "<i>${result.groupValues[1]}</i>"
        }
        return Html.fromHtml(formattedText, Html.FROM_HTML_MODE_COMPACT)
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(message: Message) {
            tvMessage.text = formatMarkdown(message.text)
        }
    }

    inner class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(message: Message) {
            tvMessage.text = formatMarkdown(message.text)
        }
    }
}
